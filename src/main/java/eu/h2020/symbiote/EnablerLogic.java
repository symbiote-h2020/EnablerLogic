package eu.h2020.symbiote;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.messaging.properties.EnablerLogicProperties;

@Service
public class EnablerLogic {
    private static final Logger log = LoggerFactory.getLogger(EnablerLogic.class);
    
    private RabbitManager rabbitManager;

    private EnablerLogicProperties props;
    
    public EnablerLogic(RabbitManager rabbitManager, EnablerLogicProperties props) {
        this.rabbitManager = rabbitManager;
        this.props = props;
    }

    public ResourceManagerAcquisitionStartResponse queryResourceManager(ResourceManagerTaskInfoRequest...requests) {
        for(ResourceManagerTaskInfoRequest request: requests) {
            log.info("sending message to ResourceManager: {}", request);
        }
        
        ResourceManagerAcquisitionStartRequest request = new ResourceManagerAcquisitionStartRequest();
        request.setResources(Arrays.asList(requests));

        ResourceManagerAcquisitionStartResponse response = (ResourceManagerAcquisitionStartResponse) 
                rabbitManager.sendRpcMessage(props.getExchange().getResourceManager().getName(), 
                        props.getKey().getResourceManager().getStartDataAcquisition(), 
                        request);
        
        log.info("Received resourceIds from ResourceManager");
        return response;
    }
    
    public void sendAsyncMessageToEnablerLogic(String enablerName, Object msg) {
    		rabbitManager.sendMessage(props.getEnablerLogicExchange().getName(), 
    		        generateAsyncEnablerLogicRoutingKey(), 
    		        msg);
    }

    private String generateAsyncEnablerLogicRoutingKey() {
        return props.getKey().getEnablerLogic().getAsyncMessageToEnablerLogic()+"." + 
                props.getEnablerName();
    }
}
