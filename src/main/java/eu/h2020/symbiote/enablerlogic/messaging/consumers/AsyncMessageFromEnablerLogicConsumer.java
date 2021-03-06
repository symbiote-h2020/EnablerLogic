package eu.h2020.symbiote.enablerlogic.messaging.consumers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import eu.h2020.symbiote.enablerlogic.messaging.LoggingTrimHelper;
import eu.h2020.symbiote.enablerlogic.messaging.WrongRequestException;

@Component
public class AsyncMessageFromEnablerLogicConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncMessageFromEnablerLogicConsumer.class);

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
        consumers.remove(clazz.getName());
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(autoDelete="true", arguments= 
                {@Argument(name = "x-message-ttl", value="#{enablerLogicProperties.rabbitConnection.replyTimeout}", type="java.lang.Integer")}),
            exchange = @Exchange(
                    value = "#{enablerLogicProperties.enablerLogicExchange.name}", 
                    type = "#{enablerLogicProperties.enablerLogicExchange.type}", 
                    durable="#{enablerLogicProperties.enablerLogicExchange.durable}",
                    autoDelete="#{enablerLogicProperties.enablerLogicExchange.autodelete}",
                    internal="#{enablerLogicProperties.enablerLogicExchange.internal}",
                    ignoreDeclarationExceptions = "true" 
                    ),
            key = "#{enablerLogicProperties.key.enablerLogic.asyncMessageToEnablerLogic}.#{enablerLogicProperties.enablerName}"
        ),
        containerFactory = "noRequeueRabbitContainerFactory"
    )
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void receivedAsyncMessage(Message msg) throws IOException {
        LOG.info("Consumer receivedAsyncMessage: " + LoggingTrimHelper.logMsg(msg));

        Object request = messageConverter.fromMessage(msg);
        String className = request.getClass().getName();
        
        Consumer consumer = consumers.get(className);
        if(consumer == null) {
            WrongRequestException exception = new WrongRequestException(
                "Asynchronous consumer can not find consumer for handling this request type.",
                request,
                className);
            LOG.error("Can not handle request.", exception);
            return;
        }

        consumer.accept(request);
    }
}
