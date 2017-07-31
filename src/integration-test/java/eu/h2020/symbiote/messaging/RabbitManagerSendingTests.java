package eu.h2020.symbiote.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static com.revinate.assertj.json.JsonPathAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.core.ReceiveAndReplyCallback;
import org.springframework.amqp.core.ReceiveAndReplyMessageCallback;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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
import io.arivera.oss.embedded.rabbitmq.bin.RabbitMqPlugins;
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
    
    private static EmbeddedRabbitMq rabbitMq;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private RabbitManager rabbitManager;
    
    @Autowired
    private ConnectionFactory factory;
    
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
    
    
    /**
     * Cleaning up after previous test and creation of new connection and channel to RabbitMQ.
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
    		rabbitManager.sendMessage(EXCHANGE_NAME, RECEIVING_ROUTING_KEY, "Some text");
    	
    		// then
    		Message message = rabbitTemplate.receive(RECEIVING_QUEUE_NAME, 10_000);
    		assertNotNull(message);
    		assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).isEqualTo("Some text");
	}
    
    @Test
	public void sendingAcyncObject_shouldReceiveAsyncObject() throws Exception {
		// given
    		ModelObject sendObject = new ModelObject("joe", 25);
    	
    		// when
    		rabbitManager.sendMessage(EXCHANGE_NAME, RECEIVING_ROUTING_KEY, sendObject);
    	
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
    		rabbitManager.sendMessage(EXCHANGE_NAME, RECEIVING_ROUTING_KEY, sendObject);
    	
    		// then
    		Message message = rabbitTemplate.receive(RECEIVING_QUEUE_NAME, 10_000);
    		assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
    		assertThat(message.getMessageProperties().getHeaders().get("__TypeId__"))
    			.isEqualTo("eu.h2020.symbiote.messaging.RabbitManagerSendingTests$ModelObject");
    		
    		String json = new String(message.getBody(), StandardCharsets.UTF_8);
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
	
    @Test
    public void sendingRPC_shouldWaitForResponseInText() throws Exception{
    		// given
	    	String sendMessage = "Some RPC message";
	    	Thread t = new Thread(() -> {
	    		log.info("receiving thread started");
	    		rabbitTemplate.setReceiveTimeout(20_000);
	    		rabbitTemplate.receiveAndReply(RECEIVING_QUEUE_NAME, new ReceiveAndReplyMessageCallback() {
					@Override
					public Message handle(Message msg) {
							log.info("receive thread received {}", msg);
							String r = new String(msg.getBody(), StandardCharsets.UTF_8) + "!!!";
							Message rmsg = new Message(r.getBytes(StandardCharsets.UTF_8),
									MessagePropertiesBuilder.newInstance().setContentType("plain/text").build());
							log.info("returning {}", rmsg);
							return rmsg;
					}
				});
	    		log.info("************** receiving thread finished");
	    	});
	    	t.start();
    	
    		// when
		String response = rabbitManager.sendRpcMessage(EXCHANGE_NAME, RECEIVING_ROUTING_KEY, sendMessage);
    	
	    	assertThat(response).isEqualTo(sendMessage + "!!!");
    }
    
    public static interface ReceiveAndReplyModelObjectCallback extends ReceiveAndReplyCallback<ModelObject, ModelObject> {}
    
    @Test
    public void sendingRPC_shouldWaitForResponseInJSONObject() throws Exception{
	    	// given
	    	ModelObject sendObject = new ModelObject("bob", 17);
	    	Thread t = new Thread(() -> {
	    		log.info("receiving thread started");
	    		rabbitTemplate.setReceiveTimeout(20_000);
	    		rabbitTemplate.receiveAndReply(RECEIVING_QUEUE_NAME, new ReceiveAndReplyModelObjectCallback() {
	    			@Override
				public ModelObject handle(ModelObject payload) {
	    				log.info("receive thread received {}", payload);
	    				ModelObject rmsg = new ModelObject("sponge " + payload.getName(), 18);
	    				log.info("returning {}", rmsg);
	    				return rmsg;
	    			}
	    		});
	    		log.info("************** receiving thread finished");
	    	});
	    	t.start();
	    	
	    	// when
	    	ModelObject respObject = (ModelObject) rabbitManager.sendRpcMessage(EXCHANGE_NAME, RECEIVING_ROUTING_KEY, sendObject);
	    	
	    	assertThat(respObject.getName()).isEqualTo("sponge bob");
	    	assertThat(respObject.getAge()).isEqualTo(18);
    }
}