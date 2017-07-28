package eu.h2020.symbiote.messaging;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.annotation.RabbitBootstrapConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;

import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.messaging.properties.EnablerLogicExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;

/**
 * Test for RPC communication with Resource Manager.
 * @author PetarKrivic
 *
 */
@RunWith(SpringRunner.class)
@Import(RabbitManager.class)
@EnableConfigurationProperties({EnablerLogicExchangeProperties.class, RoutingKeysProperties.class, RabbitConnectionProperties.class})
public class RabbitManagerSendingTests {
	private static Log log = LogFactory.getLog(RabbitManagerSendingTests.class);
	private static final int RABBIT_STARTING_TIMEOUT = 10_000;

	private Connection connection;
    private Channel channel;
    
    private String resourceManagerQueueName = "queueName";
    private String resourceManagerExchangeName = "exchangeName";
    
    private String resourceManagerResponse = "ResourceManagerResponse";
	
    private static EmbeddedRabbitMq rabbitMq;
	
    @BeforeClass
    public static void startRabbit() {
	    	EmbeddedRabbitMqConfig config = new EmbeddedRabbitMqConfig.Builder()
	    			.rabbitMqServerInitializationTimeoutInMillis(RABBIT_STARTING_TIMEOUT)
	    			.build();
	    	rabbitMq = new EmbeddedRabbitMq(config);
	    	rabbitMq.start();
    }
    
    @AfterClass
    public static void stopRabbit() {
    		rabbitMq.stop();
    }
    
    /**
     * Creation of new connection and channel to RabbitMQ.
     * Resource Manager Exchange and Queue setup.
     * Resource Manager consumer setup.
     * @throws TimeoutException
     */
	@Before
	public void initialize() throws TimeoutException {
		
        try {
        	ConnectionFactory factory = new ConnectionFactory();
        	connection = factory.newConnection();
            channel = connection.createChannel();
            
            //ResourceManager exchange
            channel.exchangeDeclare(resourceManagerExchangeName,
            		"topic",
                    true,
                    false,
                    false,
                    null);
            
            channel.queueDeclare(resourceManagerQueueName, true, false, false, null);
            channel.queueBind(resourceManagerQueueName, resourceManagerExchangeName, resourceManagerQueueName);

            log.info("ResourceManager waiting for EnablerLogic messages....");

            Consumer consumer = new ResourceManagerMock(channel);
            channel.basicConsume(resourceManagerQueueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * Testing the synchronous RPC communication with ResourceManager.
	 * @throws Exception
	 */
    @Test
    public void testSync() throws Exception{
    	
        String response = sendRpcMessage(resourceManagerExchangeName, resourceManagerQueueName, "EnablerLogicRequest");
    	assertEquals(resourceManagerResponse, response);   	    	
    }
    
    /**
     * Removal of declared queue and exchange.
     * @throws IOException
     * @throws TimeoutException
     */
    @After
    public void cleanup() throws IOException, TimeoutException{
    	//channel.queueDelete(resourceManagerQueueName);
    	//channel.exchangeDelete(resourceManagerExchangeName);
    	//this.channel.close();
    	//this.connection.close();
    }
    
    /**
     * Resource Manager consumer mock.
     */
    class ResourceManagerMock extends DefaultConsumer{

		public ResourceManagerMock(Channel channel) {
			super(channel);
		}
		
		@Override
	    public void handleDelivery(String consumerTag, Envelope envelope,
	                               AMQP.BasicProperties properties, byte[] body)
	            throws IOException {
			
	        String message = new String(body, "UTF-8");
	        log.info("ResourceManager received EnablerLogic's message: '" + message + "'");

	        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
	                .Builder()
	                .correlationId(properties.getCorrelationId())
	                .build();

	        this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, resourceManagerResponse.getBytes());
	        log.info("ResourceManager sent response: '" + resourceManagerResponse + "'");

	        this.getChannel().basicAck(envelope.getDeliveryTag(), false);
	    }
    }
    
    /**
     * Method for sending RPC message.
     * @param exchangeName
     * @param routingKey
     * @param message
     * @return
     */
    public String sendRpcMessage(String exchangeName, String routingKey, String message) {
        try {
            log.info("Sending RPC message: " + message);

            String replyQueueName = "amq.rabbitmq.reply-to";
            String correlationId = UUID.randomUUID().toString();
            
            AMQP.BasicProperties props = new AMQP.BasicProperties()
                    .builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            QueueingConsumer consumer = new QueueingConsumer(channel);
            this.channel.basicConsume(replyQueueName, true, consumer);

            String responseMsg = null;

            this.channel.basicPublish(exchangeName, routingKey, props, message.getBytes());
            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery(20000);
                if (delivery == null) {
                    log.info("Timeout in response retrieval");
                    return null;
                }

                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                    responseMsg = new String(delivery.getBody());
                    break;
                }
            }

            log.info("Response received: " + responseMsg);
            return responseMsg;
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}