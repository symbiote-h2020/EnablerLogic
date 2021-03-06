package eu.h2020.symbiote.enablerlogic.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@ConfigurationProperties(prefix = "rabbit.exchange", ignoreInvalidFields = true)
public class ExchangeProperties {
    private FullExchangeProperties enablerLogic = new FullExchangeProperties(
        "symbIoTe.enablerLogic", "topic", true, false, false);

    private EcxhangeNameProperty resourceManager = new EcxhangeNameProperty("symbIoTe.resourceManager");
    private EcxhangeNameProperty enablerPlatformProxy = new EcxhangeNameProperty("symbIoTe.enablerPlatformProxy");

    @Getter
    @Setter
    @AllArgsConstructor
    public static class EcxhangeNameProperty {
        private String name;
    }
}
