package eu.h2020.symbiote.messaging;

import com.google.gson.Gson;
import com.rabbitmq.client.*;

import eu.h2020.symbiote.messaging.consumers.DataAppearedConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Bean used to manage internal communication using RabbitMQ.
 * It is responsible for declaring exchanges and using routing keys from centralized config server.
 * <p>
 * Created by mateuszl
 */
@Component
public class RabbitManager {

    private static Log log = LogFactory.getLog(RabbitManager.class);

    @Value("${rabbit.host}")
    private String rabbitHost;
    @Value("${rabbit.username}")
    private String rabbitUsername;
    @Value("${rabbit.password}")
    private String rabbitPassword;
    
    @Value("${rabbit.exchange.enablerLogic.name}")
    private String enablerLogicExchangeName;
    @Value("${rabbit.exchange.enablerLogic.type}")
    private String enablerLogicExchangeType;
    @Value("${rabbit.exchange.enablerLogic.durable}")
    private boolean enablerLogicExchangeDurable;
    @Value("${rabbit.exchange.enablerLogic.autodelete}")
    private boolean enablerLogicExchangeAutodelete;
    @Value("${rabbit.exchange.enablerLogic.internal}")
    private boolean enablerLogicExchangeInternal;
    
    @Value("${rabbit.routingKey.enablerLogic.acquireMeasurements}")
    private String acquireMeasurementsRoutingKey;
    
    @Value("${rabbit.routingKey.enablerLogic.dataAppeared}")
    private String dataAppearedRoutingKey;
    
    private Connection connection;
    
    @Autowired 
    private AutowireCapableBeanFactory beanFactory;

    public RabbitManager() {
    }

    /**
     * Initiates connection with Rabbit server using parameters from ConfigProperties
     *
     * @throws IOException
     * @throws TimeoutException
     */
    public Connection getConnection() throws IOException, TimeoutException {
        if (connection == null) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(this.rabbitHost);
            factory.setUsername(this.rabbitUsername);
            factory.setPassword(this.rabbitPassword);
            this.connection = factory.newConnection();
        }
        return this.connection;
    }

    /**
     * Method creates channel and declares Rabbit exchanges.
     * It triggers start of all consumers used in Registry communication.
     */
    public void init() {
        Channel channel = null;
        log.info("Rabbit is being initialized!");

        try {
            getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        if (connection != null) {
            try {
                channel = this.connection.createChannel();

                channel.exchangeDeclare(this.enablerLogicExchangeName,
                        this.enablerLogicExchangeType,
                        this.enablerLogicExchangeDurable,
                        this.enablerLogicExchangeAutodelete,
                        this.enablerLogicExchangeInternal,
                        null);

                startConsumers();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeChannel(channel);
            }
        }
    }

    /**
     * Cleanup method for rabbit - set on pre destroy
     */
    @PreDestroy
    public void cleanup() {
        //FIXME check if there is better exception handling in @predestroy method
        log.info("Rabbit cleaned!");
        try {
            Channel channel;
            if (this.connection != null && this.connection.isOpen()) {
                channel = connection.createChannel();
                channel.queueUnbind("enablerLogicAcquireMeasurements", this.enablerLogicExchangeName, this.acquireMeasurementsRoutingKey);
                channel.queueUnbind("enablerLogicDataAppeared", this.enablerLogicExchangeName, this.acquireMeasurementsRoutingKey);
                channel.queueDelete("enablerLogicDataAppeared");
                channel.queueDelete("enablerLogicAcquireMeasurements");
                closeChannel(channel);
                this.connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method gathers all of the rabbit consumer starter methods
     */
    public void startConsumers() {
        try {
            startConsumerOfAcquireMeasurements();
            startConsumerOfDataAppeared();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void sendPlaceholderMessage(String placeholder) { // arg should be object instead of String, e.g. Resource
//        Gson gson = new Gson();
//        String message = gson.toJson(placeholder);
//        sendMessage(this.placeholderExchangeName, this.placeholderRoutingKey, message);
//        log.info("- placeholder message sent");
//    }

    public void sendCustomMessage(String exchange, String routingKey, String objectInJson) {
        sendMessage(exchange, routingKey, objectInJson);
        log.info("- Custom message sent");
    }

    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to -placeholder- requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfAcquireMeasurements() throws InterruptedException, IOException {
        String queueName = "enablerLogicAcquireMeasurements";
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.enablerLogicExchangeName, this.acquireMeasurementsRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for Placeholder messages....");

            Consumer consumer = new PlaceholderConsumer(channel, this);
            beanFactory.autowireBean(consumer);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to -placeholder- requests.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfDataAppeared() throws InterruptedException, IOException {
        String queueName = "enablerLogicDataAppeared";
        Channel channel;
        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.enablerLogicExchangeName, this.dataAppearedRoutingKey);
//            channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("creating DataAppearedConsumer....");

            DataAppearedConsumer consumer = new DataAppearedConsumer(channel);
            beanFactory.autowireBean(consumer);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method publishes given message to the given exchange and routing key.
     * Props are set for correct message handle on the receiver side.
     *
     * @param exchange   name of the proper Rabbit exchange, adequate to topic of the communication
     * @param routingKey name of the proper Rabbit routing key, adequate to topic of the communication
     * @param message    message content in JSON String format
     */
    private void sendMessage(String exchange, String routingKey, String message) {
        AMQP.BasicProperties props;
        Channel channel = null;
        try {
            channel = this.connection.createChannel();
            props = new AMQP.BasicProperties()
                    .builder()
                    .contentType("application/json")
                    .build();

            channel.basicPublish(exchange, routingKey, props, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeChannel(channel);
        }
    }

    /**
     * Closes given channel if it exists and is open.
     *
     * @param channel rabbit channel to close
     */
    private void closeChannel(Channel channel) {
        try {
            if (channel != null && channel.isOpen())
                channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}