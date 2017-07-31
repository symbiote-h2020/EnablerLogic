package eu.h2020.symbiote.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties.EnablerLogicKeys;
import lombok.Data;

@Data
@ConfigurationProperties(prefix="rabbit.exchange.resourceManager", ignoreInvalidFields = true)
public class ResourceManagerExchangeProperties {

	private String name;
/*
	@Value("${rabbit.routingKey.resourceManager.startDataAcquisition}")
    private String resourceManager_startDataAcquisition_key;
    
    @Value("${rabbit.exchange.enablerPlatformProxy.name}")
    private String platformProxyExchangeName;
*/
	
	public String getExchangeName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getStartDataAcquisitionKey() {
		// TODO Auto-generated method stub
		return null;
	}

}
