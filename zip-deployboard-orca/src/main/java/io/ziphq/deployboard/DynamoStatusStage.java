package io.ziphq.deployboard;

import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import org.pf4j.Extension;

@Extension
public class DynamoStatusStage implements StageDefinitionBuilder {
    public void taskGraph(StageExecution stage, TaskNode.Builder builder) {
        builder.withTask("dynamoStatus", DynamoStatusTask.class);
    }
}
