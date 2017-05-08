package eu.h2020.symbiote.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.repository.EnablerLogicDataAppearedMessageRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * Consumer of the Data Appeared Message.
 * Created by Petar Krivic on 04/04/2017.
 */
public class DataAppearedConsumer extends DefaultConsumer {
	
	@Autowired
	EnablerLogicDataAppearedMessageRepository repository;

    private static Log log = LogFactory.getLog(DataAppearedConsumer.class);
    RabbitManager rabbitManager;
    
    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     * @param channel the channel to which this consumer is attached
     *
     */
    public DataAppearedConsumer(Channel channel, RabbitManager rabbitManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
    		AMQP.BasicProperties properties, byte[] body) throws IOException {
    	
        String msg = new String(body, "UTF-8");
        log.info( "Consume DataAppeared message: " + msg );

        //store the received data to MongoDB...
        ObjectMapper om = new ObjectMapper();
        EnablerLogicDataAppearedMessage dataAppearedMessage = om.readValue(msg, EnablerLogicDataAppearedMessage.class);
        		
        repository.save(dataAppearedMessage);
        
        //In case answer is needed
        /**
        try {
            ObjectMapper mapper = new ObjectMapper();
            //TODO read proper value and handle received data message
            String dataAppearedMessage = mapper.readValue(msg, String.class);

            log.debug( "Sending response to the sender....");

            byte[] responseBytes = mapper.writeValueAsBytes("Response");

            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(properties.getCorrelationId())
                    .build();
            this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, responseBytes);
            log.debug("-> Message sent back");

            this.getChannel().basicAck(envelope.getDeliveryTag(), false);

        } catch( JsonParseException | JsonMappingException e ) {
            log.error("Error occurred when parsing Resource object JSON: " + msg, e);
        } catch( IOException e ) {
            log.error("I/O Exception occurred when parsing Resource object" , e);
        }
        **/
    }
}