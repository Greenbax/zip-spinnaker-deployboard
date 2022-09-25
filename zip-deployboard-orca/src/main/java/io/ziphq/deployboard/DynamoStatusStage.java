package io.ziphq.deployboard;

import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class DynamoStatusStage implements StageDefinitionBuilder {
    private Logger logger = LoggerFactory.getLogger(DynamoStatusTask.class);

    public void taskGraph(StageExecution stage, TaskNode.Builder builder) {
        logger.info("AAAAAAAAAAAA in stage");
        builder.withTask("dynamoStatus", DynamoStatusTask.class);
    }
}
