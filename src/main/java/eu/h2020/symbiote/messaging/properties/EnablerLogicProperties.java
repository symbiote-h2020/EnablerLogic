package eu.h2020.symbiote.messaging.properties;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Component
public class EnablerLogicProperties {
	@Getter
	private RabbitConnectionProperties rabbitConnection;
	private ExchangeProperties exchangeProperties;
	private RoutingKeysProperties elRoutingKeyProperties;

	public FullExchangeProperties getEnablerLogicExchange() {
		return exchangeProperties.getEnablerLogic();
	}
	
	public RoutingKeysProperties getKey() {
		return elRoutingKeyProperties;
	}
	
	public ExchangeProperties getExchange() {
		return exchangeProperties;
	}
}
