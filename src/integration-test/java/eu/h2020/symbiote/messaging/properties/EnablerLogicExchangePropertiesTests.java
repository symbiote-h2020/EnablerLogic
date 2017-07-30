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
@TestPropertySource(locations="classpath:custom.properties")
public class EnablerLogicExchangePropertiesTests {
	@Autowired
	private EnablerLogicExchangeProperties props;
	
	@Test
	public void shouldLoadFirstLevelProperties() {
		assertThat(props.getName()).isEqualTo("el");
		assertThat(props.getType()).isEqualTo("t");
		assertFalse(props.isDurable());
		assertTrue(props.isAutodelete());
		assertTrue(props.isInternal());
	}
}
