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
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.rabbitmq.client.Channel;

import eu.h2020.symbiote.EnablerLogic;
import eu.h2020.symbiote.ProcessingLogic;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.bin.RabbitMqPlugins;

@RunWith(SpringRunner.class)
@Import({TestingRabbitConfig.class,
    EnablerLogicProperties.class, 
    DataAppearedConsumer.class})
@EnableConfigurationProperties({RabbitConnectionProperties.class, ExchangeProperties.class, RoutingKeysProperties.class})
@TestPropertySource(locations="classpath:empty.properties")
public class DataAppearedConsumerTest {
    private static final int RABBIT_STARTING_TIMEOUT = 10_000;

    @Configuration
    @EnableRabbit
    public static class ProcessingLogicConfig {
        @Bean
        public ProcessingLogicTestImpl processingLogic() {
            return new ProcessingLogicTestImpl();
        }
    }
    
    private static EmbeddedRabbitMq rabbitMq;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    EnablerLogicProperties props;

    @Autowired
    DataAppearedConsumer dataAppearedConsumer;
    
    @Autowired
    ProcessingLogicTestImpl processingLogic;

    public static class ProcessingLogicTestImpl implements ProcessingLogic {
            public EnablerLogicDataAppearedMessage dataAppearedMessage;

        @Override
        public void init(EnablerLogic enablerLogic) {
        }

        @Override
        public void measurementReceived(EnablerLogicDataAppearedMessage dataAppearedMessage) {
            this.dataAppearedMessage = dataAppearedMessage;
        }
            
    }
    
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
    public void dataAppeared_shouldReceiveMessage() {
        // given
        EnablerLogicDataAppearedMessage message = new EnablerLogicDataAppearedMessage();
        message.setTaskId("taskId");
        
        // when
        rabbitTemplate.convertAndSend(
                props.getEnablerLogicExchange().getName(), 
                props.getKey().getEnablerLogic().getDataAppeared(), 
                message);
        
        // then
        await().until(() -> processingLogic.dataAppearedMessage != null);
        EnablerLogicDataAppearedMessage receivedMessage = processingLogic.dataAppearedMessage;
        assertThat(receivedMessage.getTaskId()).isEqualTo("taskId");
    }
}
