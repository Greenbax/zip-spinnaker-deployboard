package io.ziphq.deployboard;

import java.util.Collections;
import org.pf4j.Extension;
import com.netflix.spinnaker.gate.api.extension.ApiExtension;
import com.netflix.spinnaker.gate.api.extension.HttpRequest;
import com.netflix.spinnaker.gate.api.extension.HttpResponse;

@Extension
public class HelloWorldExtension implements ApiExtension {
    public String id() {
        return "test";
    }

    public boolean handles(HttpRequest httpRequest) {
        return true;
    }

    public HttpResponse handle(HttpRequest httpRequest) {
        return HttpResponse.of(200, Collections.emptyMap(), "Hello world!");
    }
}
