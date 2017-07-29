package eu.h2020.symbiote.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static com.revinate.assertj.json.JsonPathAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.rabbitmq.client.Channel;

import eu.h2020.symbiote.messaging.properties.EnablerLogicExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * Test for RPC communication with Resource Manager.
 * @author PetarKrivic
 *
 */
@RunWith(SpringRunner.class)
@Import({RabbitManager.class})
@EnableConfigurationProperties({EnablerLogicExchangeProperties.class, RoutingKeysProperties.class, RabbitConnectionProperties.class})
public class RabbitManagerSendingTests {
	private static Logger log = LoggerFactory.getLogger(RabbitManagerSendingTests.class);

	private static final int RABBIT_STARTING_TIMEOUT = 10_000;
	
	private static final String EXCHANGE_NAME = "exchangeName";
	private static final String RECEIVING_QUEUE_NAME = "queueName";
	private static final String RECEIVING_ROUTING_KEY = "receivingQueue";
	
	@Configuration
	public static class RabbitConfig {
		@Bean
		public ConnectionFactory connectionFactory() {
			return new CachingConnectionFactory("localhost");
		}
		
		@Bean
		public RabbitTemplate rabbitTemaplate(ConnectionFactory connectionFactory) {
			RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
			rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
			return rabbitTemplate;
		}
	}

	private Connection connection;
    private Channel channel;
    
    private String resourceManagerResponse = "ResourceManagerResponse";
	
    private static EmbeddedRabbitMq rabbitMq;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private RabbitManager rabbitManeger;
    
    @Autowired
    private ConnectionFactory factory;
    
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
     * @throws Exception
     */
	@Before
	public void initialize() throws Exception {
		connection = factory.createConnection();
		channel = connection.createChannel(false);
		
		cleanRabbitResources();
		createRabbitResources();
	}

	private void createRabbitResources() throws IOException {
		channel.exchangeDeclare(EXCHANGE_NAME, "topic", true, true, false, null);
		channel.queueDeclare(RECEIVING_QUEUE_NAME, true, false, false, null);
		channel.queueBind(RECEIVING_QUEUE_NAME, EXCHANGE_NAME, RECEIVING_ROUTING_KEY);
	}

	private void cleanRabbitResources() throws IOException {
		channel.queueDelete(RECEIVING_QUEUE_NAME);
		channel.exchangeDelete(EXCHANGE_NAME);
	}
    
    @Test
	public void sendingAcyncMessage_shouldReceiveAsyncMessage() throws Exception {
		// given

    		// when
    		rabbitManeger.sendMessage(EXCHANGE_NAME, RECEIVING_ROUTING_KEY, "Some text");
    	
    		// then
    		Message message = rabbitTemplate.receive(RECEIVING_QUEUE_NAME, 10_000);
    		assertNotNull(message);
    		assertThat(new String(message.getBody(), "UTF-8")).isEqualTo("Some text");
	}
    
    @Test
	public void sendingAcyncObject_shouldReceiveAsyncObject() throws Exception {
		// given
    		ModelObject sendObject = new ModelObject("joe", 25);
    	
    		// when
    		rabbitManeger.sendMessage(EXCHANGE_NAME, RECEIVING_ROUTING_KEY, sendObject);
    	
    		// then
    		Object o = rabbitTemplate.receiveAndConvert(RECEIVING_QUEUE_NAME, 10_000);
    		assertNotNull(o);
    		assertThat(o).isInstanceOf(ModelObject.class);
    		ModelObject receivedObject = (ModelObject) o;
    		assertThat(receivedObject).isEqualToComparingFieldByField(sendObject);
	}
    
    @Test
	public void sendingAcyncObject_shouldReceiveAsyncJSON() throws Exception {
		// given
    		ModelObject sendObject = new ModelObject("joe", 25);
    	
    		// when
    		rabbitManeger.sendMessage(EXCHANGE_NAME, RECEIVING_ROUTING_KEY, sendObject);
    	
    		// then
    		Message message = rabbitTemplate.receive(RECEIVING_QUEUE_NAME, 10_000);
    		assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
    		assertThat(message.getMessageProperties().getHeaders().get("__TypeId__"))
    			.isEqualTo("eu.h2020.symbiote.messaging.RabbitManagerSendingTests$ModelObject");
    		
    		String json = new String(message.getBody(), "UTF-8");
		assertThat(json).isEqualTo("{\"name\":\"joe\",\"age\":25}");
		DocumentContext ctx = JsonPath.parse(json);
		assertThat(ctx).jsonPathAsString("name").isEqualTo("joe");
		assertThat(ctx).jsonPathAsInteger("age").isEqualTo(25);
	}

    @Data 
    @NoArgsConstructor 
    @AllArgsConstructor
    public static class ModelObject {
    		private String name;
    		private int age;
    }
	
	/**
	 * Testing the synchronous RPC communication with ResourceManager.
	 * @throws Exception
	 */
    @Test
    public void testSync() throws Exception{
    	
        String response = sendRpcMessage(EXCHANGE_NAME, RECEIVING_QUEUE_NAME, "EnablerLogicRequest");
    		//assertEquals(resourceManagerResponse, response);
    		
    }
    
    /**
     * Removal of declared queue and exchange.
     * @throws IOException
     * @throws TimeoutException
     */
    @After
    public void cleanup() throws IOException, TimeoutException{
//    		channel.queueDelete(RECEIVING_QUEUE_NAME);
//    		channel.exchangeDelete(EXCHANGE_NAME);
//    		this.channel.close();
//    		this.connection.close();
    }
    
    /**
     * Method for sending RPC message.
     * @param exchangeName
     * @param routingKey
     * @param message
     * @return
     */
    public String sendRpcMessage(String exchangeName, String routingKey, String message) {
//        try {
//            log.info("Sending RPC message: " + message);
//
//            String replyQueueName = "amq.rabbitmq.reply-to";
//            String correlationId = UUID.randomUUID().toString();
//            
//            AMQP.BasicProperties props = new AMQP.BasicProperties()
//                    .builder()
//                    .correlationId(correlationId)
//                    .replyTo(replyQueueName)
//                    .build();
//
//            QueueingConsumer consumer = new QueueingConsumer(channel);
//            this.channel.basicConsume(replyQueueName, true, consumer);
//
//            String responseMsg = null;
//
//            this.channel.basicPublish(exchangeName, routingKey, props, message.getBytes());
//            while (true) {
//                QueueingConsumer.Delivery delivery = consumer.nextDelivery(20000);
//                if (delivery == null) {
//                    log.info("Timeout in response retrieval");
//                    return null;
//                }
//
//                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
//                    responseMsg = new String(delivery.getBody());
//                    break;
//                }
//            }
//
//            log.info("Response received: " + responseMsg);
//            return responseMsg;
//        } catch (IOException | InterruptedException e) {
//            log.error(e.getMessage(), e);
//        }
        return null;
    }
}