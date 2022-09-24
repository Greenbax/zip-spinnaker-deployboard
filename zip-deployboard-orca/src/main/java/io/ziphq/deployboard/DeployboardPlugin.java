package io.ziphq.deployboard;

import com.netflix.spinnaker.orca.api.pipeline.graph.StageGraphBuilder;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode.Builder;

import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

public class DeployboardPlugin extends Plugin {

    public DeployboardPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Logger logger = LoggerFactory.getLogger(DeployboardPlugin.class);

    @Override
    public void start() {
        logger.info("DeployboardPlugin.start()");
    }

    @Override
    public void stop() {
        logger.info("DeployboardPlugin.stop()");
    }
}

@Extension
class DynamoStatusStage implements StageDefinitionBuilder {
    public void taskGraph(StageExecution stage, Builder builder) {
        builder.withTask("dynamoStatus", DynamoStatusTask.class);
    }
}

@Extension
class DynamoStatusTask implements Task {
    private String dynamoTableName = "zip-spinnaker-ci-deploys";

    public TaskResult execute(StageExecution stage) {
        DeployboardContext context = stage.mapTo(DeployboardContext.class);
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
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
