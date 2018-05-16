package eu.h2020.symbiote.enablerlogic;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.awaitility.Awaitility.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.enabler.messaging.model.NotEnoughResourcesAvailable;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.messaging.RabbitManager;
import eu.h2020.symbiote.enablerlogic.messaging.RegistrationHandlerClientService;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.AsyncMessageFromEnablerLogicConsumer;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.NotEnoughResourcesConsumer;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.SyncMessageFromEnablerLogicConsumer;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.TestingRabbitConfig;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.NotEnoughResourcesConsumerTest.ProcessingLogicTestImpl;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.PluginProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.RoutingKeysProperties;

@Ignore
@RunWith(SpringRunner.class)
@Import({EnablerLogicErrorInInitializationTest.ProcessingLogicConfig.class,
	EnablerLogicConfiguration.class})
//@EnableConfigurationProperties({RabbitConnectionProperties.class, 
//	ExchangeProperties.class, RoutingKeysProperties.class, PluginProperties.class})
@TestPropertySource(locations = "classpath:empty.properties")
@DirtiesContext
public class EnablerLogicErrorInInitializationTest {

    @Configuration
    public static class ProcessingLogicConfig {
        @Bean
        public ProcessingLogicErrorTestImpl processingLogic() {
            return new ProcessingLogicErrorTestImpl();
        }
        
        @Bean
        public EnablerLogicProperties enablerLogicProperties() {
            return new EnablerLogicProperties();
        }
        
        @Bean 
        public EnablerLogic enablerLogic() {
        	return Mockito.mock(EnablerLogic.class);
        }
        
        @Bean 
        public SyncMessageFromEnablerLogicConsumer syncConsumer() {
        	return Mockito.mock(SyncMessageFromEnablerLogicConsumer.class);
        }

        @Bean 
        public AsyncMessageFromEnablerLogicConsumer asyncConsumer() {
        	return Mockito.mock(AsyncMessageFromEnablerLogicConsumer.class);
        }
    }
    
    public static class ProcessingLogicErrorTestImpl extends ProcessingLogicAdapter {
        public volatile boolean thrownException = false;

		@Override
		public void initialization(EnablerLogic enablerLogic) {
			thrownException = true;
			throw new IllegalArgumentException();
		}
    }
    
    @Autowired
    ConfigurableApplicationContext ctx;
    
    @Autowired
    ProcessingLogicErrorTestImpl pl;
    
    @Test
    public void testThatErrorIn_isCalledInConsumer() throws Exception {
    	await().atMost(5, TimeUnit.SECONDS).until(() -> pl.thrownException);
    	await().atMost(5, TimeUnit.SECONDS).until(() -> !ctx.isRunning());
    }
}
