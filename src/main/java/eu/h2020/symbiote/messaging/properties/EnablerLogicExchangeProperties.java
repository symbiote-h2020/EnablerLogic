package eu.h2020.symbiote.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix="rabbit.exchange.enablerLogic", ignoreInvalidFields = true)
public class EnablerLogicExchangeProperties {
		private String name = "symbIoTe.enablerLogic";
		private String type = "topic";
		private boolean durable = true;
		private boolean autodelete = false;
		private boolean internal = false;		
}
