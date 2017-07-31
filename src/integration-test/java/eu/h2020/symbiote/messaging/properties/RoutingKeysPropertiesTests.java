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
@TestPropertySource(locations="classpath:custom.properties")
public class RoutingKeysPropertiesTests {
	@Autowired
	private RoutingKeysProperties props;
	
	@Test
	public void shouldLoadFirstLevelProperties() {
		assertThat(props.getEnablerLogic().getAcquireMeasurements()).isEqualTo("c_am");
		assertThat(props.getEnablerLogic().getDataAppeared()).isEqualTo("c_da");
	}
}
