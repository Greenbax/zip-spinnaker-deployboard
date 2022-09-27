package io.ziphq.deployboard;

import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import org.pf4j.Extension;

@Extension
public class DynamoStatusTask implements Task {
    private String dynamoTableName = "zip-spinnaker-ci-deploys";

    public TaskResult execute(StageExecution stage) {
        DynamoStatusContext context = stage.mapTo(DynamoStatusContext.class);
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).withCredentials(WebIdentityTokenCredentialsProvider.create()).build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(dynamoTableName);

        // On successful build, update deployed field in dynamo.
        if (context.getSuccess() != null) {
            // Set currently deployed build as last deployed.
            GetItemSpec getItemSpec = new GetItemSpec()
                    .withPrimaryKey("branch", context.getBranch(), "status", "DEPLOYED");
            Item lastDeploy = table.getItem(getItemSpec);
            if (lastDeploy != null) {
                String lastDeployedImage = lastDeploy.getString("image");
                Item item = new Item()
                        .withPrimaryKey("branch", context.getBranch(), "status", "LAST_DEPLOYED")
                        .withString("image", lastDeployedImage);
                table.putItem(item);
            }

            // Update currently deployed build.
            Item item = new Item()
                    .withPrimaryKey("branch", context.getBranch(), "status", "DEPLOYED")
                    .withString("image", context.getImage());
            table.putItem(item);

            // Delete deploying build.
            DeleteItemSpec deleteSpec = new DeleteItemSpec()
                    .withPrimaryKey("branch", context.getBranch(), "status", "DEPLOYING");
            table.deleteItem(deleteSpec);
        } else {
            // Otherwise, update deploying field and add 10 min TTL.
            Item item = new Item()
                    .withPrimaryKey("branch", context.getBranch(), "status", "DEPLOYING")
                    .withString("image", context.getImage())
                    .withLong("ttl", (System.currentTimeMillis() / 1000L + 10 * 60));
            table.putItem(item);
        }
        return TaskResult.SUCCEEDED;
    }
}
