package eu.h2020.symbiote.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "rabbit.exchange.resourceManager", ignoreInvalidFields = true)
public class ResourceManagerExchangeProperties {

    private String name;

    public String getExchangeName() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getStartDataAcquisitionKey() {
        // TODO Auto-generated method stub
        return null;
    }
}
