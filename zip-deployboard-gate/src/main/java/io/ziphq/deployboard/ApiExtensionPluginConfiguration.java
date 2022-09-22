package io.armory.secrets.silly;

import com.netflix.spinnaker.kork.plugins.api.PluginConfiguration;
import lombok.Data;

@Data
@PluginConfiguration("ziphq.deployboard")
public class ApiExtensionPluginConfig {

    private String test;

}