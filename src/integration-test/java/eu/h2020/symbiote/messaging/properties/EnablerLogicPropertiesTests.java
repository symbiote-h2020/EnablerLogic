package eu.h2020.symbiote.messaging.properties;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties({ExchangeProperties.class, RoutingKeysProperties.class, RabbitConnectionProperties.class})
@TestPropertySource(locations="classpath:custom.properties")
@Import(EnablerLogicProperties.class)
public class EnablerLogicPropertiesTests {
	@Autowired
	private EnablerLogicProperties props;
	
	@Test
	public void shouldHaveLoaded3Properties() {
		assertThat(props.getExchange()).isNotNull();
		assertThat(props.getKey()).isNotNull();
		assertThat(props.getKey()).isNotNull();
	}

	@Test
	public void shouldHaveLoadedFullExchangePropertiesForEnablerLogic() {
		assertThat(props.getEnablerLogicExchange().getName()).isEqualTo("c_el");
		assertThat(props.getEnablerLogicExchange().getType()).isEqualTo("c_t");
		assertThat(props.getEnablerLogicExchange().isDurable()).isFalse();
	}
	
	@Test
	public void shouldHaveLoadedExchangeNames() {
		assertThat(props.getExchange().getEnablerPlatformProxy().getName()).isEqualTo("c_pp");
		assertThat(props.getExchange().getResourceManager().getName()).isEqualTo("c_rm");
	}

	@Test
	public void shouldHaveLoadedRoutingKeys() {
		assertThat(props.getKey().getEnablerLogic().getAcquireMeasurements()).isEqualTo("c_am");
		assertThat(props.getKey().getEnablerPlatformProxy().getAcquisitionStartRequested()).isEqualTo("c_asr");
		assertThat(props.getKey().getResourceManager().getStartDataAcquisition()).isEqualTo("c_sda");
	}
}