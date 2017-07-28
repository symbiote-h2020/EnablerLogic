package eu.h2020.symbiote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoResponse;
import eu.h2020.symbiote.messaging.RabbitManager;

@Component
public class EnablerLogic {
    private static Log log = LogFactory.getLog(EnablerLogic.class);

    private RabbitManager rabbitManager;
    
    @Value("${rabbit.exchange.resourceManager.name}")
    private String resourceManagerExchangeName;
    @Value("${rabbit.routingKey.resourceManager.startDataAcquisition}")
    private String resourceManager_startDataAcquisition_key;
    
    @Value("${rabbit.exchange.enablerPlatformProxy.name}")
    private String platformProxyExchangeName;

    
    public EnablerLogic(RabbitManager rabbitManager) {
		this.rabbitManager = rabbitManager;
    }
	
	public ResourceManagerAcquisitionStartResponse queryResourceManager(ResourceManagerTaskInfoRequest...requests) throws EnablerLogicException {
		
        try {
			ObjectMapper objectMapper = new ObjectMapper();
			for(ResourceManagerTaskInfoRequest request: requests) {
				log.info("sending message to ResourceManager: " + objectMapper.writeValueAsString(request));
			}
			
			ResourceManagerAcquisitionStartRequest request = new ResourceManagerAcquisitionStartRequest();
			request.setResources(Arrays.asList(requests));

			
			String responseString = rabbitManager.sendRpcMessage(resourceManagerExchangeName, 
					resourceManager_startDataAcquisition_key, 
					objectMapper.writeValueAsString(request));
			
			ResourceManagerAcquisitionStartResponse response = objectMapper.readValue(responseString.trim(), 
					ResourceManagerAcquisitionStartResponse.class);

			log.info("Received resourceIds from ResourceManager");
			return response;
		} catch (JsonProcessingException e) {
			throw new EnablerLogicException("Can not convert object from/to JSON.", e);
		} catch (IOException e) {
			throw new EnablerLogicException("Problem with RabbitMQ communication.", e);
		}
	}
}
