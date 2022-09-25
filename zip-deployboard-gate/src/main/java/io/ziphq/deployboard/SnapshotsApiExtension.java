package io.ziphq.deployboard;

import java.util.*;

import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.regions.Regions;
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
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

@Data(staticConstructor = "of")
class GetBuildsResult {
    private final ArrayList<Build> builds;
}

@Data(staticConstructor = "of")
class Build {
    private final String branch;
    private final Integer buildNumber;
    private final String dockerImage;
    private final ArrayList<Commit> commits;
    private String status;

    public void addCommit(Commit commit) {
        this.commits.add(commit);
    }

    public void setStatus(String deployedImage, String deployingImage) {
        if (this.getDockerImage() == deployedImage) {
            this.setStatus("DEPLOYED");
        } else if (this.getDockerImage() == deployingImage) {
            this.setStatus("DEPLOYING");
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
    private final Integer ts;
}

@Data(staticConstructor = "of")
class BranchStatus {
    private String deployed;
    private String deploying;
}

@Extension
public class SnapshotsApiExtension implements ApiExtension {
    private String snapshotTableName = "zip-spinnaker-ci-builds";
    private String deployStatusTableName = "zip-spinnaker-ci-status";
    private Integer buildLimit = 25;

    public String id() {
        return "snapshots";
    }

    public boolean handles(HttpRequest httpRequest) {
        return true;
    }
    public HttpResponse handle(HttpRequest httpRequest) {
        Map<String, String> params = httpRequest.getParameters();
        String branch = "prod";
        String author = "";
        String lastSortKeySeen = "None";

        if (params.containsKey("branch")) {
            branch = params.get("branch");
        }
        if (params.containsKey("author")) {
            author = params.get("author");
        }
        if (params.containsKey("lastSortKeySeen")) {
            lastSortKeySeen = params.get("lastSortKeySeen");
        }

        BranchStatus branchStatus = this.queryBranchStatus(branch);
        String deployedImage = branchStatus.getDeployed();
        String deployingImage = branchStatus.getDeploying();

        QuerySpec spec = new QuerySpec().withScanIndexForward(false)
                .withKeyConditionExpression("branch = :branch_name and build#ts#author#sha < :sort_key")
                .withFilterExpression("author CONTAINS :author_name")
                .withValueMap(new ValueMap()
                        .withString(":branch_name", branch)
                        .withString(":sort_key", lastSortKeySeen)
                        .withString(":author_name", author)
                )
                .withMaxPageSize(100);

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).withCredentials(WebIdentityTokenCredentialsProvider.create()).build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(snapshotTableName);
        ItemCollection<QueryOutcome> items = table.query(spec);
        ArrayList<Build> builds = new ArrayList<>();
        Boolean buildLimitReached = false;
        for (Page<Item, QueryOutcome> page : items.pages()) {
            // Process each item on the current page
            Iterator<Item> item = page.iterator();
            while (item.hasNext()) {
                Item dbItem = item.next();
                Integer buildNumber = Integer.parseInt(dbItem.getString("branch"));
                Commit commit = Commit.of(
                        dbItem.getString("author"),
                        dbItem.getString("sha"),
                        dbItem.getString("msg"),
                        Integer.parseInt(dbItem.getString("ts"))
                );
                // Add a new build if buildNumber is different
                if (builds.size() == 0 || builds.get(builds.size() - 1).getBuildNumber() != buildNumber) {
                    if (builds.size() == buildLimit) {
                        buildLimitReached = true;
                        break;
                    }
                    Build build = Build.of(
                            dbItem.getString("branch"),
                            buildNumber,
                            dbItem.getString("dockerTag"),
                            new ArrayList<>()
                    );
                    build.setStatus(deployedImage, deployingImage);
                    builds.add(build);
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
        QuerySpec spec = new QuerySpec().withKeyConditionExpression("branch = :branch_name")
                .withValueMap(new ValueMap()
                        .withString(":branch_name", branch)
                );

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
            if (status == "DEPLOYED") {
                res.setDeployed(item.getString("image"));
            } else if (status == "DEPLOYING") {
                res.setDeploying(item.getString("image"));
            }
        }
        return res;
    }
}
