package io.ziphq.deployboard;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spinnaker.gate.api.extension.ApiExtension;
import com.netflix.spinnaker.gate.api.extension.HttpRequest;
import com.netflix.spinnaker.gate.api.extension.HttpResponse;

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

@Extension
public interface HelloWorldExtension extends ApiExtension {
  @Nonnull
  String id(
    return "test"
  );

  default boolean handles(@Nonnull HttpRequest httpRequest) {
    return true;
  }

  @Nonnull
  default HttpResponse handle(@Nonnull HttpRequest httpRequest) {
    return HttpResponse.of(200, Collections.emptyMap(), "Hello world!");
  }
}