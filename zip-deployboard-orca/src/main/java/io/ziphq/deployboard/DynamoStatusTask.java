package io.ziphq.deployboard;

import java.util.*;

import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class DynamoStatusTask implements Task {
    private String dynamoTableName = "zip-spinnaker-ci-deploys";
    private Logger logger = LoggerFactory.getLogger(DynamoStatusTask.class);

    public TaskResult execute(StageExecution stage) {
        logger.info("AAAAAAAAA in task");
        Map<String, Object> maybeContext = stage.getContext();
        logger.info(String.format("maybeContext branch: %s", maybeContext.get("branch")));
        DynamoStatusContext context = stage.mapTo(DynamoStatusContext.class);
        logger.info(String.format("context: %s", context));
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).withCredentials(WebIdentityTokenCredentialsProvider.create()).build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(dynamoTableName);

        // On successful build, update deployed field in dynamo.
        if (context.getSuccess()) {
            Item item = new Item()
                    .withPrimaryKey("branch", context.getBranch(), "status", "DEPLOYED")
                    .withString("image", context.getImage());
            table.putItem(item);
            DeleteItemSpec deleteSpec = new DeleteItemSpec()
                    .withPrimaryKey("branch", context.getBranch(), "status", "DEPLOYING");
            table.deleteItem(deleteSpec);
        } else {
            // Otherwise, update deploying field and add 10 min TTL.
            Item item = new Item()
                    .withPrimaryKey("branch", context.getBranch(), "status", "DEPLOYING")
                    .withString("image", context.getImage())
                    .withLong("ttl", (System.currentTimeMillis() / 1000L + 10 * 60 * 60));
            table.putItem(item);
        }
        return TaskResult.SUCCEEDED;
    }
}
