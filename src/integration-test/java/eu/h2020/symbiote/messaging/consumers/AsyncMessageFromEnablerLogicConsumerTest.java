package eu.h2020.symbiote.messaging.consumers;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.bin.RabbitMqPlugins;
import lombok.AllArgsConstructor;
import lombok.Getter;

@RunWith(SpringRunner.class)
@Import({TestingRabbitConfig.class,
        EnablerLogicProperties.class, 
        AsyncMessageFromEnablerLogicConsumer.class})
@EnableConfigurationProperties({RabbitConnectionProperties.class, ExchangeProperties.class, RoutingKeysProperties.class})
@TestPropertySource(locations="classpath:empty.properties")
public class AsyncMessageFromEnablerLogicConsumerTest {
    private static final int RABBIT_STARTING_TIMEOUT = 10_000;

    private static EmbeddedRabbitMq rabbitMq;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    EnablerLogicProperties props;

    @Autowired
    AsyncMessageFromEnablerLogicConsumer consumer;
    
    @AllArgsConstructor
    public static class CustomMessage {
        @Getter
        private String message;
    }
    
    CustomMessage receivedMessage = null;
    
    @BeforeClass
    public static void startEmbeddedRabbit() throws Exception {
            EmbeddedRabbitMqConfig config = new EmbeddedRabbitMqConfig.Builder()
                    .rabbitMqServerInitializationTimeoutInMillis(RABBIT_STARTING_TIMEOUT)
                    .build();

            cleanupVarDir(config);
            
        rabbitMq = new EmbeddedRabbitMq(config);
            rabbitMq.start();

            RabbitMqPlugins rabbitMqPlugins = new RabbitMqPlugins(config);
        rabbitMqPlugins.enable("rabbitmq_management");
        rabbitMqPlugins.enable("rabbitmq_tracing");
    }

    private static void cleanupVarDir(EmbeddedRabbitMqConfig config) throws IOException {
        File varDir = new File(config.getAppFolder(), "var");
        if(varDir.exists())
            FileUtils.cleanDirectory(varDir);
    }
    
    @AfterClass
    public static void stopEmbeddedRabbit() {
            rabbitMq.stop();
    }
    
    @Test
    public void asyncMessage_shouldCallLambda() {
        // given
        CustomMessage message = new CustomMessage("some custom text");
        consumer.registerReceiver(CustomMessage.class, (m) -> receivedMessage = m);
        
        // when
        rabbitTemplate.convertAndSend(
                props.getEnablerLogicExchange().getName(), 
                props.getKey().getEnablerLogic().getAsyncMessageToEnablerLogic() + "." + props.getEnablerName(), 
                message);
        
        // then
        await().until(() -> receivedMessage != null);
        assertThat(receivedMessage.getMessage()).isEqualTo(message.getMessage());
    }
}
