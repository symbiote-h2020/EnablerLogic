package eu.h2020.symbiote.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@ConfigurationProperties(prefix = "rabbit.routingKey", ignoreInvalidFields = true)
public class RoutingKeysProperties {

    private EnablerLogicKeys enablerLogic;
    private ResourceManagerKeys resourceManager;
    private PlatformProxyKeys enablerPlatformProxy;

    public RoutingKeysProperties() {
        enablerLogic = new EnablerLogicKeys();
        resourceManager = new ResourceManagerKeys();
        enablerPlatformProxy = new PlatformProxyKeys();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EnablerLogicKeys {
        private String acquireMeasurements = "symbIoTe.enablerLogic.acquireMeasurements";
        private String dataAppeared = "symbIoTe.enablerLogic.dataAppeared";
        private String asyncMessageToEnablerLogic = "symbIoTe.enablerLogic.asyncMessageToEnablerLogic";
        private String syncMessageToEnablerLogic = "symbIoTe.enablerLogic.syncMessageToEnablerLogic";

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResourceManagerKeys {
        private String startDataAcquisition = "symbIoTe.resourceManager.startDataAcquisition";
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlatformProxyKeys {
        private String acquisitionStartRequested = "symbIoTe.enablerPlatformProxy.acquisitionStartRequested";
        private String acquisitionStopRequested = "symbIoTe.enablerPlatformProxy.acquisitionStopRequested";

    }
}
