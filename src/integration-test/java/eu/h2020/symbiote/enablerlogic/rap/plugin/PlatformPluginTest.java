package eu.h2020.symbiote.enablerlogic.rap.plugin;

import static eu.h2020.symbiote.util.json.JsonPathAssert.assertThat;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.rabbitmq.client.Channel;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.enablerlogic.messaging.RabbitManager;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.EmbeddedRabbitFixture;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.TestingRabbitConfig;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.RoutingKeysProperties;
import eu.h2020.symbiote.enablerlogic.rap.plugin.PlatformPlugin;
import eu.h2020.symbiote.enablerlogic.rap.resources.RapDefinitions;

@RunWith(SpringRunner.class)
@Import({RabbitManager.class,
    TestingRabbitConfig.class,
    EnablerLogicProperties.class})
@EnableConfigurationProperties({RabbitConnectionProperties.class, ExchangeProperties.class, RoutingKeysProperties.class})
public class PlatformPluginTest extends EmbeddedRabbitFixture {
    private static final String EXCHANGE_NAME = RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_IN;
    private static final String RECEIVING_QUEUE_NAME = RapDefinitions.PLUGIN_REGISTRATION_QUEUE;
    private static final String RECEIVING_ROUTING_KEY = RapDefinitions.PLUGIN_REGISTRATION_KEY;
    
    private static final int RECEIVE_TIMEOUT = 20_000;

    
    @Configuration
    public static class TestConfiguration {
        @Bean
        public PlatformPlugin platfromPlugin(RabbitManager manager) {
            return new PlatformPlugin(manager, "platId", false, true) {
                
                @Override
                public void writeResource(String resourceId, String body) { }
                
                @Override
                public void unsubscribeResource(String resourceId) { }
                
                @Override
                public void subscribeResource(String resourceId) { }
                
                @Override
                public List<Observation> readResourceHistory(String resourceId) { return null; }
                
                @Override
                public List<Observation> readResource(String resourceId) { return null; }
            };
        }
    }
    
    @Autowired
    private PlatformPlugin platformPlugin;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ConnectionFactory factory;
    
    private Connection connection;
    private Channel channel;

    @Before
    public void initialize() throws Exception {
        connection = factory.createConnection();
        channel = connection.createChannel(false);

        cleanRabbitResources();
        createRabbitResources();
    }

    private void createRabbitResources() throws IOException {
        channel.exchangeDeclare(EXCHANGE_NAME, "topic", true, true, false, null);
        channel.queueDeclare(RECEIVING_QUEUE_NAME, true, false, false, null);
        channel.queueBind(RECEIVING_QUEUE_NAME, EXCHANGE_NAME, RECEIVING_ROUTING_KEY);
    }

    private void cleanRabbitResources() throws IOException {
        channel.queueDelete(RECEIVING_QUEUE_NAME);
        channel.exchangeDelete(EXCHANGE_NAME);
    }


    @Test
    public void platformRegistration_shouldSendMessageToRAP() throws Exception {
        //given
    
        // when
        platformPlugin.start();
    
        //then
        Message message = rabbitTemplate.receive(RECEIVING_QUEUE_NAME, RECEIVE_TIMEOUT);
        assertNotNull(message);
        
        String jsonBody = new String(message.getBody(), StandardCharsets.UTF_8);
        DocumentContext ctx = JsonPath.parse(jsonBody);
        assertThat(ctx).jsonPathAsString("type").isEqualTo("REGISTER_PLUGIN");
        assertThat(ctx).jsonPathAsString("platformId").isEqualTo("platId");
        assertThat(ctx).jsonPathAsBoolean("hasFilters").isFalse();
        assertThat(ctx).jsonPathAsBoolean("hasNotifications").isTrue();
    }
}
