package eu.h2020.symbiote.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="rabbit.exchange.enablerLogic", ignoreInvalidFields = true)
public class EnablerLogicExchangeProperties {
		private String name = "symbIoTe.enablerLogic";
		private String type = "topic";
		private boolean durable = true;
		private boolean autodelete = false;
		private boolean internal = false;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public boolean isDurable() {
			return durable;
		}
		public void setDurable(boolean durable) {
			this.durable = durable;
		}
		public boolean isAutodelete() {
			return autodelete;
		}
		public void setAutodelete(boolean autodelete) {
			this.autodelete = autodelete;
		}
		public boolean isInternal() {
			return internal;
		}
		public void setInternal(boolean internal) {
			this.internal = internal;
		}
}
