package eu.h2020.symbiote.messaging.properties;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(RoutingKeysProperties.class)
@TestPropertySource(locations="classpath:empty.properties")
public class RoutingKeysDefaultPropertiesTests {
	@Autowired
	private RoutingKeysProperties props;
	
	@Test
	public void shouldLoadFirstLevelProperties() {
		assertThat(props.getEnablerLogic().getAcquireMeasurements()).isEqualTo("symbIoTe.enablerLogic.acquireMeasurements");
		assertThat(props.getEnablerLogic().getDataAppeared()).isEqualTo("symbIoTe.enablerLogic.dataAppeared");
		assertThat(props.getEnablerLogic().getAsyncMessageToEnablerLogic()).isEqualTo("symbIoTe.enablerLogic.asyncMessageToEnablerLogic");
		assertThat(props.getEnablerLogic().getSyncMessageToEnablerLogic()).isEqualTo("symbIoTe.enablerLogic.syncMessageToEnablerLogic");
	}
}
