package eu.h2020.symbiote.messaging.consumers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class AsyncMessageFromEnablerLogicConsumer {
    private static final Logger log = LoggerFactory.getLogger(AsyncMessageFromEnablerLogicConsumer.class);
    
    @SuppressWarnings("rawtypes")
    private Map<String, Consumer> consumers;
    
    private MessageConverter messageConverter;

    public AsyncMessageFromEnablerLogicConsumer() {
        consumers = new HashMap<>();
        messageConverter = new Jackson2JsonMessageConverter();
    }
    
    public <O> void registerReceiver(Class<O> clazz, Consumer<O> consumer) {
        consumers.put(clazz.getName(), consumer);
    }

    public <O> void unregisterReceiver(Class<O> clazz) {
        // TODO test 
        //consumers.remove(clazz);
    }
    
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue,
        exchange = @Exchange(value = "#{enablerLogicProperties.enablerLogicExchange.name}", type="topic"),
        key = "#{enablerLogicProperties.key.enablerLogic.asyncMessageToEnablerLogic}.#{enablerLogicProperties.enablerName}"
    ))
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void receivedAsyncMessage(Message msg, @Header("__TypeId__") String className) throws IOException {
        log.info("Consumer receivedAsyncMessage: " + msg);
        
        Consumer consumer = consumers.get(className);
        consumer.accept(messageConverter.fromMessage(msg));
    }
}
