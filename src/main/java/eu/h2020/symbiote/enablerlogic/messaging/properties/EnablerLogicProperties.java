package eu.h2020.symbiote.enablerlogic.messaging.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component("enablerLogicProperties")
public class EnablerLogicProperties {
    @Getter
    @Value("${spring.application.name}")
    private String enablerName = "DefaultEnablerName";

    @Getter
    private RabbitConnectionProperties rabbitConnection;
    private ExchangeProperties exchangeProperties;
    private RoutingKeysProperties routingKeyProperties;
// TODO
//    private PluginProperties pluginProperties;

// TODO
//    @Data
//    @ConfigurationProperties(prefix = "enablerLogic.plugin", ignoreInvalidFields = true)
//    public static class PluginProperties {
//        // TODO test loadanja, default vrijednost, krive vrijednosti
//        @Getter
//        private boolean filtersSupported = false;
//        
//        // TODO test loadanja, default vrijednost, krive vrijednosti
//        @Getter
//        private boolean notificationsSupported = false;
//        
//        
//    }
    
    public EnablerLogicProperties() {
        rabbitConnection = new RabbitConnectionProperties();
        exchangeProperties = new ExchangeProperties();
        routingKeyProperties = new RoutingKeysProperties();
// TODO        pluginProperties = new PluginProperties();
    }

    @Autowired
    public EnablerLogicProperties(RabbitConnectionProperties rabbitConnection, ExchangeProperties exchangeProperties,
            RoutingKeysProperties routingKeyProperties) {
        this.rabbitConnection = rabbitConnection;
        this.exchangeProperties = exchangeProperties;
        this.routingKeyProperties = routingKeyProperties;
    }

    public FullExchangeProperties getEnablerLogicExchange() {
        return exchangeProperties.getEnablerLogic();
    }

    public RoutingKeysProperties getKey() {
        return routingKeyProperties;
    }

    public ExchangeProperties getExchange() {
        return exchangeProperties;
    }
    
// TODO
//    public PluginProperties getPlugin() {
//        return pluginProperties;
//    }
}
