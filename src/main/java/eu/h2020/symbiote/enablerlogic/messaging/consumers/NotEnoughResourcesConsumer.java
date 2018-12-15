package eu.h2020.symbiote.enablerlogic.messaging.consumers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import eu.h2020.symbiote.enabler.messaging.model.NotEnoughResourcesAvailable;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.enablerlogic.messaging.LoggingTrimHelper;

public class NotEnoughResourcesConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(NotEnoughResourcesConsumer.class);

    private ProcessingLogic processingLogic;

    public NotEnoughResourcesConsumer(ProcessingLogic processingLogic) {
        this.processingLogic = processingLogic;
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
            key = "#{enablerLogicProperties.key.enablerLogic.notEnoughResources}"
        ),
        containerFactory = "noRequeueRabbitContainerFactory"
    )
    public void dataAppeared(NotEnoughResourcesAvailable notEnoughResourcesAvailableMessage) throws IOException {
        LOG.info("Consumer NotEnoughResourcesAvailable message: " + LoggingTrimHelper.logToString(notEnoughResourcesAvailableMessage));
        processingLogic.notEnoughResources(notEnoughResourcesAvailableMessage);
    }
}
