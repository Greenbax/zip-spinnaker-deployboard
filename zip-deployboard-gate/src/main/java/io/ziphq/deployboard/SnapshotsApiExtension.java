package io.ziphq.deployboard;

import java.util.*;

import org.pf4j.Extension;
import lombok.Data;

import com.netflix.spinnaker.gate.api.extension.ApiExtension;
import com.netflix.spinnaker.gate.api.extension.HttpRequest;
import com.netflix.spinnaker.gate.api.extension.HttpResponse;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.regions.Regions;

@Data(staticConstructor = "of")
class GetBuildsResult {
    private final ArrayList<Build> builds;
}

@Data(staticConstructor = "of")
class Build {
    private final String branch;
    private final Integer buildNumber;
    private final String dockerImage;
    private final Integer ttl;
    private ArrayList<Commit> commits = new ArrayList<>();
    private String status;

    public void addCommit(Commit commit) {
        this.commits.add(commit);
    }

    public void setDeployStatus(BranchStatus status) {
        if (this.getDockerImage().equals(status.getDeployed())) {
            this.setStatus("DEPLOYED");
        } else if (this.getDockerImage().equals(status.getDeploying())) {
            this.setStatus("DEPLOYING");
        } else if (this.getDockerImage().equals(status.getLastDeployed())) {
            this.setStatus("LAST_DEPLOYED");
        } else {
            this.setStatus("NOT_DEPLOYED");
        }
    }
}

@Data(staticConstructor = "of")
class Commit {
    private final String author;
    private final String sha;
    private final String message;
    private final Long ts;
}

@Data(staticConstructor = "of")
class BranchStatus {
    private String deployed = "";
    private String deploying = "";
    private String lastDeployed = "";
}

@Extension
public class SnapshotsApiExtension implements ApiExtension {
    private String snapshotTableName = "zip-spinnaker-ci-snapshots";
    private String deployStatusTableName = "zip-spinnaker-ci-deploys";
    private Integer buildLimit = 25;
    private String dockerPrefix = "242230929264.dkr.ecr.us-east-2.amazonaws.com/evergreen-server:";

    public String id() {
        return "snapshots";
    }

    public boolean handles(HttpRequest httpRequest) {
        return true;
    }
    public HttpResponse handle(HttpRequest httpRequest) {
        Map<String, String> params = httpRequest.getParameters();
        String branch = "prod";
        String query = "";
        String lastSortKeySeen = "None";

        if (params.containsKey("branch")) {
            branch = params.get("branch");
        }
        if (params.containsKey("query")) {
            query = params.get("query");
        }
        if (params.containsKey("lastSortKeySeen")) {
            lastSortKeySeen = params.get("lastSortKeySeen");
        }

        BranchStatus branchStatus = this.queryBranchStatus(branch);
        String filterExpression = "contains(author,:query) or contains(msg,:query)";
        ValueMap valueMap = new ValueMap()
                .withString(":branch_name", branch)
                .withString(":sort_key", lastSortKeySeen)
                .withString(":query", query);
        if (!branchStatus.getDeployed().equals("")) {
            filterExpression += " or contains(dockerTag,:deployed_tag)";
            valueMap.withString(":deployed_tag", branchStatus.getDeployed().replaceFirst(this.dockerPrefix, ""));
        }
        if (!branchStatus.getDeploying().equals("")) {
            filterExpression += " or contains(dockerTag,:deploying_tag)";
            valueMap.withString(":deploying_tag", branchStatus.getDeploying().replaceFirst(this.dockerPrefix, ""));
        }
        if (!branchStatus.getLastDeployed().equals("")) {
            filterExpression += " or contains(dockerTag,:last_deployed_tag)";
            valueMap.withString(":last_deployed_tag", branchStatus.getLastDeployed().replaceFirst(this.dockerPrefix, ""));
        }

        QuerySpec spec = new QuerySpec().withScanIndexForward(false)
                .withKeyConditionExpression("branch = :branch_name and #sort_key_name < :sort_key")
                .withFilterExpression(filterExpression)
                .withNameMap(new NameMap()
                        .with("#sort_key_name", "ttl#build#ts#author#sha")
                )
                .withValueMap(valueMap)
                .withMaxPageSize(100);

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).withCredentials(WebIdentityTokenCredentialsProvider.create()).build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(snapshotTableName);
        ItemCollection<QueryOutcome> items = table.query(spec);
        ArrayList<Build> builds = new ArrayList<>();
        Integer currBuildNumber = 0;
        Boolean buildLimitReached = false;
        for (Page<Item, QueryOutcome> page : items.pages()) {
            // Process each item on the current page
            Iterator<Item> item = page.iterator();
            while (item.hasNext()) {
                Item dbItem = item.next();
                Integer buildNumber = Integer.parseInt(dbItem.getString("build"));
                Commit commit = Commit.of(
                        dbItem.getString("author"),
                        dbItem.getString("sha"),
                        dbItem.getString("msg"),
                        Long.parseLong(dbItem.getString("ts"))
                );
                // Add a new build if buildNumber is different
                if (!buildNumber.equals(currBuildNumber)) {
                    if (buildLimit.equals(builds.size())) {
                        buildLimitReached = true;
                        break;
                    }
                    Build build = Build.of(
                            dbItem.getString("branch"),
                            buildNumber,
                            String.format("%s%s", this.dockerPrefix, dbItem.getString("dockerTag")),
                            dbItem.getInt("ttl")
                    );
                    build.setDeployStatus(branchStatus);
                    builds.add(build);
                    currBuildNumber = buildNumber;
                }
                builds.get(builds.size() - 1).addCommit(commit);
            }

            if (buildLimitReached) {
                break;
            }
        }

        return HttpResponse.of(200, Collections.emptyMap(), GetBuildsResult.of(builds));
    }

    public BranchStatus queryBranchStatus(String branch) {
        QuerySpec spec = new QuerySpec().withHashKey("branch", branch);

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).withCredentials(WebIdentityTokenCredentialsProvider.create()).build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(deployStatusTableName);
        ItemCollection<QueryOutcome> items = table.query(spec);
        Iterator<Item> iterator = items.iterator();
        Item item = null;

        BranchStatus res = BranchStatus.of();
        while (iterator.hasNext()) {
            item = iterator.next();
            String status = item.getString("status");
            if (status.equals("DEPLOYED")) {
                res.setDeployed(item.getString("image"));
            } else if (status.equals("DEPLOYING")) {
                res.setDeploying(item.getString("image"));
            } else if (status.equals("LAST_DEPLOYED")) {
                res.setLastDeployed(item.getString("image"));
            }
        }
        return res;
    }
}
