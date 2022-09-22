package io.ziphq.deployboard;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiExtensionPlugin extends Plugin {

    public ApiExtensionPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Logger logger = LoggerFactory.getLogger(ApiExtensionPlugin.class);

    @Override
    public void start() {
        logger.info("ApiExtensionPlugin.start()");
    }

    @Override
    public void stop() {
        logger.info("ApiExtensionPlugin.stop()");
    }
}
