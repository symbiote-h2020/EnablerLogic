package eu.h2020.symbiote.enablerlogic.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "enablerLogic.plugin")
public class PluginProperties {
    private boolean filtersSupported = false;
    private boolean notificationsSupported = false;
}