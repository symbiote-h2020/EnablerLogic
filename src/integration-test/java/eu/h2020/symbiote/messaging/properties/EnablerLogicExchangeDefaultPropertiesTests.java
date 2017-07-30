package eu.h2020.symbiote.messaging.properties;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.messaging.properties.EnablerLogicExchangeProperties;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(EnablerLogicExchangeProperties.class)
@TestPropertySource(locations="classpath:empty.properties")
public class EnablerLogicExchangeDefaultPropertiesTests {
	@Autowired
	private EnablerLogicExchangeProperties props;
	
	@Test
	public void shouldLoadFirstLevelProperties() {
		assertThat(props.getName()).isEqualTo("symbIoTe.enablerLogic");
		assertThat(props.getType()).isEqualTo("topic");
		assertTrue(props.isDurable());
		assertFalse(props.isAutodelete());
		assertFalse(props.isInternal());
	}
}
