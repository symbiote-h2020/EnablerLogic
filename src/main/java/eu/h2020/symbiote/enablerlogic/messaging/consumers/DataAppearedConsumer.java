package eu.h2020.symbiote.enablerlogic.messaging.consumers;

import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.enablerlogic.messaging.LoggingTrimHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;

import org.springframework.stereotype.Component;


import java.io.IOException;

/**
 * Consumer of the Data Appeared Message.
 * Created by Petar Krivic on 04/04/2017.
 */

@Component
public class DataAppearedConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(DataAppearedConsumer.class);

    private ProcessingLogic processingLogic;

    public DataAppearedConsumer(ProcessingLogic processingLogic) {
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
            key = "#{enablerLogicProperties.key.enablerLogic.dataAppeared}"
        ),
        containerFactory = "noRequeueRabbitContainerFactory"
    )
    public void dataAppeared(EnablerLogicDataAppearedMessage dataAppearedMessage) throws IOException {
        LOG.info("Consumer DataAppeared message: " + LoggingTrimHelper.logToString(dataAppearedMessage));
        processingLogic.measurementReceived(dataAppearedMessage);
    }
}
