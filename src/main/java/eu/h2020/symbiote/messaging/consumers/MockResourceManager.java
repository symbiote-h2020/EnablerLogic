package eu.h2020.symbiote.messaging.consumers;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.PlaceholderResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * RabbitMQ Consumer implementation used for Placeholder actions
 *
 * Created by mateuszl
 */
public class MockResourceManager extends DefaultConsumer {

    private static Log log = LogFactory.getLog(MockResourceManager.class);
    private RabbitManager rabbitManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     * @param repositoryManager repository manager bean passed for persistence actions
     */
    public MockResourceManager(Channel channel,
                                           RabbitManager rabbitManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
    }

    /**
     * Called when a <code><b>basic.deliver</b></code> is received for this consumer.
     *
     * @param consumerTag the <i>consumer tag</i> associated with the consumer
     * @param envelope    packaging data for the message
     * @param properties  content header data for the message
     * @param body        the message body (opaque, client-specific byte array)
     * @throws IOException if the consumer encounters an I/O error while processing the message
     * @see Envelope
     */
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        //Gson gson = new Gson();
        String response = "";
        String message = new String(body, "UTF-8");
        log.info(" [x] Received -placeholder- message: '" + message + "'");

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(properties.getCorrelationId())
                .build();

        this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, "ajmeo vise ajmoooooo".getBytes());
        log.info("Message with status: "+ " sent back");

        this.getChannel().basicAck(envelope.getDeliveryTag(), false);
    }
}

