package eu.h2020.symbiote.messaging;

import com.rabbitmq.client.*;

import eu.h2020.symbiote.messaging.RabbitManagerSendingTests.ModelObject;
import eu.h2020.symbiote.messaging.consumers.AcquireMeasurementsConsumer;
import eu.h2020.symbiote.messaging.consumers.DataAppearedConsumer;
import eu.h2020.symbiote.messaging.properties.EnablerLogicExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Bean used to manage internal communication using RabbitMQ. It is responsible
 * for declaring exchanges and using routing keys from centralized config
 * server.
 * <p>
 * Created by mateuszl
 */
@Component
public class RabbitManager {
	private static Logger log = LoggerFactory.getLogger(RabbitManager.class);

	private RabbitTemplate rabbitTemplate;

	public RabbitManager(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
		
	}
	
//	private Connection connection;
//	private Channel channel;
//
//	@Autowired
//	private AutowireCapableBeanFactory beanFactory;
//
//	private RabbitConnectionProperties connectionPropertoes;
//	private EnablerLogicExchangeProperties enablerLogicExchangeProperties;
//	private RoutingKeysProperties routingKeysProperties;
//
//	public RabbitManager(RabbitConnectionProperties connectionPropertoes,
//			EnablerLogicExchangeProperties enablerLogicExchangeProperties,
//			RoutingKeysProperties routingKeysProperties) 
//	{
//		this.connectionPropertoes = connectionPropertoes;
//		this.enablerLogicExchangeProperties = enablerLogicExchangeProperties;
//		this.routingKeysProperties = routingKeysProperties;
//	}

	/**
	 * Initiates connection with Rabbit server using parameters from
	 * ConfigProperties
	 *
	 * @throws IOException
	 * @throws TimeoutException
	 */
//	public Connection getConnection() throws IOException, TimeoutException {
//		if (connection == null) {
//			ConnectionFactory factory = new ConnectionFactory();
//			factory.setHost(this.connectionPropertoes.getHost());
//			factory.setUsername(this.connectionPropertoes.getUsername());
//			factory.setPassword(this.connectionPropertoes.getPassword());
//			this.connection = factory.newConnection();
//		}
//		return this.connection;
//	}

	/**
	 * Method creates channel and declares Rabbit exchanges. It triggers start of
	 * all consumers used in Registry communication.
	 */
//	public void init() {
//
//		log.info("Rabbit is being initialized!");
//
//		try {
//			getConnection();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (TimeoutException e) {
//			e.printStackTrace();
//		}
//
//		if (connection != null) {
//			try {
//				channel = this.connection.createChannel();
//				channel.exchangeDeclare(this.enablerLogicExchangeProperties.getName(), 
//						this.enablerLogicExchangeProperties.getType(),
//						this.enablerLogicExchangeProperties.isDurable(), 
//						this.enablerLogicExchangeProperties.isAutodelete(),
//						this.enablerLogicExchangeProperties.isInternal(), null);
//
//				startConsumers();
//
//			} catch (IOException e) {
//				e.printStackTrace();
//			} finally {
//				// closeChannel(channel);
//			}
//		}
//	}

	/**
	 * Cleanup method for rabbit - set on pre destroy
	 */
//	@PreDestroy
//	public void cleanup() {
//		// FIXME check if there is better exception handling in @predestroy method
//		log.info("Rabbit cleaned!");
//		try {
//			Channel channel;
//			if (this.connection != null && this.connection.isOpen()) {
//				channel = connection.createChannel();
//				channel.queueUnbind("enablerLogicAcquireMeasurements", this.enablerLogicExchangeProperties.getName(),
//						this.routingKeysProperties.getEnablerLogic().getAcquireMeasurements());
//				channel.queueUnbind("enablerLogicDataAppeared", this.enablerLogicExchangeProperties.getName(),
//						this.routingKeysProperties.getEnablerLogic().getAcquireMeasurements());
//				channel.queueDelete("enablerLogicDataAppeared");
//				channel.queueDelete("enablerLogicAcquireMeasurements");
//				channel.exchangeDelete(this.enablerLogicExchangeProperties.getName());
//				closeChannel(channel);
//				this.connection.close();
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	/**
	 * Method gathers all of the rabbit consumer starter methods
	 */
//	public void startConsumers() {
//		try {
//			startConsumerOfAcquireMeasurements();
//			startConsumerOfDataAppeared();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	/**
	 * Method creates queue and binds it globally available exchange and adequate
	 * Routing Key. It also creates a consumer for messages incoming to this queue,
	 * regarding to Domain Specific Interface requests.
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 */
//	private void startConsumerOfAcquireMeasurements() throws InterruptedException, IOException {
//		String queueName = "enablerLogicAcquireMeasurements";
//		Channel channel;
//		try {
//			channel = this.connection.createChannel();
//			channel.queueDeclare(queueName, true, false, false, null);
//			channel.queueBind(queueName, 
//					this.enablerLogicExchangeProperties.getName(), 
//					this.routingKeysProperties.getEnablerLogic().getAcquireMeasurements());
//			// channel.basicQos(1); // to spread the load over multiple servers we set the
//			// prefetchCount setting
//
//			log.info("Creating AcquireMeasurementsConsumer...");
//
//			Consumer consumer = new AcquireMeasurementsConsumer(channel, this);
//			beanFactory.autowireBean(consumer);
//			channel.basicConsume(queueName, false, consumer);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	/**
	 * Method creates queue and binds it globally available exchange and adequate
	 * Routing Key. It also creates a consumer for messages incoming to this queue,
	 * regarding to Platform Proxy messages.
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 */
//	private void startConsumerOfDataAppeared() throws InterruptedException, IOException {
//		String queueName = "enablerLogicDataAppeared";
//		Channel channel;
//		try {
//			channel = this.connection.createChannel();
//			channel.queueDeclare(queueName, true, false, false, null);
//			channel.queueBind(queueName, 
//					this.enablerLogicExchangeProperties.getName(), 
//					this.routingKeysProperties.getEnablerLogic().getDataAppeared());
//			// channel.basicQos(1); // to spread the load over multiple servers we set the
//			// prefetchCount setting
//
//			log.info("Creating DataAppearedConsumer....");
//
//			DataAppearedConsumer consumer = new DataAppearedConsumer(channel, this);
//			beanFactory.autowireBean(consumer);
//			channel.basicConsume(queueName, false, consumer);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	/**
	 * Method publishes given message to the given exchange and routing key. Props
	 * are set for correct message handle on the receiver side.
	 *
	 * @param exchange
	 *            name of the proper Rabbit exchange, adequate to topic of the
	 *            communication
	 * @param routingKey
	 *            name of the proper Rabbit routing key, adequate to topic of the
	 *            communication
	 * @param message
	 *            message content in JSON String format
	 */
	public void sendMessage(String exchange, String routingKey, String message) {
		rabbitTemplate.send(exchange, routingKey, new Message(message.getBytes(StandardCharsets.UTF_8), 
				MessagePropertiesBuilder.newInstance()
					.setContentType("plain/text")
					.build()));
	}

	/**
	 * Method publishes given message to the given exchange and routing key. Props
	 * are set for correct message handle on the receiver side.
	 *
	 * @param exchange
	 *            name of the proper Rabbit exchange, adequate to topic of the
	 *            communication
	 * @param routingKey
	 *            name of the proper Rabbit routing key, adequate to topic of the
	 *            communication
	 * @param object
	 *            object content is mapped to JSON String by using Jackson2
	 */
	public void sendMessage(String exchange, String routingKey, Object obj) {
		rabbitTemplate.convertAndSend(exchange, routingKey, obj);
	}

	/**
	 * Method used to send message via RPC (Remote Procedure Call) pattern. In this
	 * implementation it covers asynchronous Rabbit communication with synchronous
	 * one, as it is used by conventional REST facade. Before sending a message, a
	 * temporary response queue is declared and its name is passed along with the
	 * message. When a consumer handles the message, it returns the result via the
	 * response queue. Since this is a synchronous pattern, it uses timeout of 20
	 * seconds. If the response doesn't come in that time, the method returns with
	 * null result.
	 *
	 * @param exchangeName
	 *            name of the exchange to send message to
	 * @param routingKey
	 *            routing key to send message to
	 * @param message
	 *            message to be sent
	 * @return response from the consumer or null if timeout occurs
	 */
	public String sendRpcMessage(String exchange, String routingKey, String stringMessage) {
		log.info("Sending RPC message: {}", stringMessage);
		
		String correlationId = UUID.randomUUID().toString();
		Message sendMessage = new Message(stringMessage.getBytes(StandardCharsets.UTF_8), 
				MessagePropertiesBuilder.newInstance()
					.setContentType("plain/text")
					.setCorrelationIdString(correlationId)
					.build()
				);
		rabbitTemplate.setReplyTimeout(20_000);
    		Message receivedMessage = rabbitTemplate.sendAndReceive(exchange, routingKey, sendMessage);
    		if(receivedMessage == null) { 
    			log.info("Timeout in RPC receiving. Send: {}", sendMessage);
    			return null;
    		}

    		log.info("RPC Response received: " + receivedMessage);

		byte[] body = receivedMessage.getBody();
		log.info("client received: {}", body);
		return new String(body, StandardCharsets.UTF_8);
	}

	public Object sendRpcMessage(String exchange, String routingKey, Object obj) {
		log.info("Sending RPC obj: {}", obj);
		
		String correlationId = UUID.randomUUID().toString();
		rabbitTemplate.setReplyTimeout(20_000);
    		Object receivedObj = rabbitTemplate.convertSendAndReceive(exchange, routingKey, obj, new CorrelationData(correlationId));
    		if(receivedObj == null) { 
    			log.info("Timeout in RPC receiving obj. Send: {}", obj);
    			return null;
    		}

    		log.info("RPC Response received obj: " + receivedObj);

		return receivedObj;
	}

	/**
	 * Closes given channel if it exists and is open.
	 *
	 * @param channel
	 *            rabbit channel to close
	 */
//	private void closeChannel(Channel channel) {
//		try {
//			if (channel != null && channel.isOpen())
//				channel.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (TimeoutException e) {
//			e.printStackTrace();
//		}
//	}
}