package eu.h2020.symbiote.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ConfigurationProperties(prefix="rabbit.routingKey", ignoreInvalidFields = true)
public class RoutingKeysProperties {

	private EnablerLogicKeys enablerLogic;
	
	public RoutingKeysProperties() {
		enablerLogic = new EnablerLogicKeys();
	}

	@Data
	@NoArgsConstructor
	public static class EnablerLogicKeys {
		private String acquireMeasurements = "symbIoTe.enablerLogic.acquireMeasurements";
		private String dataAppeared = "symbIoTe.enablerLogic.dataAppeared";
	}
}
