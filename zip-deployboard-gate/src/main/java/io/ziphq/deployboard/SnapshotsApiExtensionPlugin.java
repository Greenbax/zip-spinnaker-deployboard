package io.ziphq.deployboard;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnapshotsApiExtensionPlugin extends Plugin {

    public SnapshotsApiExtensionPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Logger logger = LoggerFactory.getLogger(SnapshotsApiExtensionPlugin.class);

    @Override
    public void start() {
        logger.info("SnapshotsApiExtensionPlugin.start()");
    }

    @Override
    public void stop() {
        logger.info("SnapshotsApiExtensionPlugin.stop()");
    }
}
