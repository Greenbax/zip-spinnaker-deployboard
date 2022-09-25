package io.ziphq.deployboard;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoStatusPlugin extends Plugin {

    public DynamoStatusPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Logger logger = LoggerFactory.getLogger(DynamoStatusPlugin.class);

    @Override
    public void start() {
        logger.info("DynamoStatusPlugin.start()");
    }

    @Override
    public void stop() {
        logger.info("DynamoStatusPlugin.stop()");
    }
}

