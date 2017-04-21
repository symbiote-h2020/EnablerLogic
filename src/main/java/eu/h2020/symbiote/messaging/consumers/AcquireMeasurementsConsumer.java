package eu.h2020.symbiote.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoResponse;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.repository.ResourceManagerTaskInfoResponseRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Consumer of the Acquire Measurements Message.
 *
 * Created by Petar Krivic on 04/04/2017.
 */
public class AcquireMeasurementsConsumer extends DefaultConsumer {
	
	@Autowired
	private ResourceManagerTaskInfoResponseRepository repository;

    private static Log log = LogFactory.getLog(AcquireMeasurementsConsumer.class);
    RabbitManager rabbitManager;
    
    String resourceManagerExchangeName = "symbIoTe.resourceManager";
    String resourceManagerQueueName = "symbIoTe.resourceManager.startDataAcquisition";
    
    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     *
     * @param channel           the channel to which this consumer is attached
     * @param rabbitManager     rabbit manager bean passed for access to messages manager
     */
    public AcquireMeasurementsConsumer(Channel channel, RabbitManager rabbitManager) {
   		super(channel);
    	this.rabbitManager = rabbitManager;
    }
    
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, 
    		AMQP.BasicProperties properties, byte[] body) throws IOException {
       
    	//received message from Domain Specific Interface
    	String msg = new String(body);
        log.debug( "Consume AcquireMeasurements message: " + msg );

        //TODO process message received from Domain Specific Interface (and return response?)
        /**
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            String acquireMeasurementsMessage = mapper.readValue(msg, String.class);

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
        
        //Generate TaskId, forward message to Resource Manager and store result to mongoDB (taskId with received resourceIDs)
        String uniqueId = UUID.randomUUID().toString();
        
        ResourceManagerTaskInfoRequest dummyTask = new ResourceManagerTaskInfoRequest();
        dummyTask.setTaskId(uniqueId);
        dummyTask.setCount(2);
        dummyTask.setLocation("symbolicLocation");
        List<String> observedProperties = new ArrayList<String>();
        observedProperties.add("temperature");
        observedProperties.add("humidity");
        dummyTask.setObservesProperty(observedProperties);
        dummyTask.setInterval(5);
        
        String uniqueId2 = UUID.randomUUID().toString();
        
        ResourceManagerTaskInfoRequest dummyTask2 = new ResourceManagerTaskInfoRequest();
        dummyTask2.setTaskId(uniqueId2);
        dummyTask2.setCount(1);
        dummyTask2.setLocation("symbolicLocation2");
        List<String> observedProperties2 = new ArrayList<String>();
        observedProperties2.add("air quality");
        dummyTask2.setObservesProperty(observedProperties2);
        dummyTask2.setInterval(30);
        
        ResourceManagerAcquisitionStartRequest dummyRequest = new ResourceManagerAcquisitionStartRequest();
        List<ResourceManagerTaskInfoRequest> tasks = new ArrayList<ResourceManagerTaskInfoRequest>();
        tasks.add(dummyTask);
        tasks.add(dummyTask2);
        dummyRequest.setResources(tasks);
        
        ObjectMapper objectMapper = new ObjectMapper();
        log.info("sending message to ResourceManager: " + objectMapper.writeValueAsString(dummyRequest));
        
        String responseString = rabbitManager.sendRpcMessage(resourceManagerExchangeName, resourceManagerQueueName, dummyRequest.toString());

        Gson gson = new Gson();
        ResourceManagerAcquisitionStartResponse response = gson.fromJson(responseString, ResourceManagerAcquisitionStartResponse.class);
        
        for (Iterator<ResourceManagerTaskInfoResponse> iter = response.getResources().iterator(); iter.hasNext();) {
            ResourceManagerTaskInfoResponse taskInfoResponse = (ResourceManagerTaskInfoResponse) iter.next();
            
            repository.insert(taskInfoResponse);
        }
    }
}