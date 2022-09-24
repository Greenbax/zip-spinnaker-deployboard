package io.ziphq.deployboard;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
