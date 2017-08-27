package eu.h2020.symbiote.messaging.properties;

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

    public EnablerLogicProperties() {
        rabbitConnection = new RabbitConnectionProperties();
        exchangeProperties = new ExchangeProperties();
        routingKeyProperties = new RoutingKeysProperties();
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
}
