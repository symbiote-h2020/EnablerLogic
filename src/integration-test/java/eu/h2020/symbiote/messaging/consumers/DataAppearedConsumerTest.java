package eu.h2020.symbiote.messaging.consumers;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.EnablerLogic;
import eu.h2020.symbiote.ProcessingLogic;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;

@RunWith(SpringRunner.class)
@Import({TestingRabbitConfig.class,
    EnablerLogicProperties.class, 
    DataAppearedConsumer.class})
@EnableConfigurationProperties({RabbitConnectionProperties.class, ExchangeProperties.class, RoutingKeysProperties.class})
@TestPropertySource(locations="classpath:empty.properties")
public class DataAppearedConsumerTest extends EmbeddedRabbitFixture {

    @Configuration
    @EnableRabbit
    public static class ProcessingLogicConfig {
        @Bean
        public ProcessingLogicTestImpl processingLogic() {
            return new ProcessingLogicTestImpl();
        }
    }
    
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
