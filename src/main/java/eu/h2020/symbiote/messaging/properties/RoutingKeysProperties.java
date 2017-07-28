package eu.h2020.symbiote.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="rabbit.routingKey", ignoreInvalidFields = true)
public class RoutingKeysProperties {

	private EnablerLogicKeys enablerLogic;
	
	public RoutingKeysProperties() {
		enablerLogic = new EnablerLogicKeys();
	}

	public EnablerLogicKeys getEnablerLogic() {
		return enablerLogic;
	}

	public void setEnablerLogic(EnablerLogicKeys enablerLogic) {
		this.enablerLogic = enablerLogic;
	}
	
	public static class EnablerLogicKeys {
		private String acquireMeasurements = "symbIoTe.enablerLogic.acquireMeasurements";
		private String dataAppeared = "symbIoTe.enablerLogic.dataAppeared";

		public String getAcquireMeasurements() {
			return acquireMeasurements;
		}

		public void setAcquireMeasurements(String acquireMeasurements) {
			this.acquireMeasurements = acquireMeasurements;
		}

		public String getDataAppeared() {
			return dataAppeared;
		}

		public void setDataAppeared(String dataAppeared) {
			this.dataAppeared = dataAppeared;
		}
	}
}
