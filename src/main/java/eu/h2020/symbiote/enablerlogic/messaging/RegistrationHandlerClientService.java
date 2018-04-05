package eu.h2020.symbiote.enablerlogic.messaging;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.client.RegistrationHandlerClient;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import feign.Feign;
import feign.Logger.Level;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

@Service
public class RegistrationHandlerClientService {
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationHandlerClientService.class);
    
    private RegistrationHandlerClient client;
    private ObjectMapper mapper;
    private boolean checkForRegistrationHandlerInEureka;
    private boolean registrationHandlerChecked = false;
    private String registrationHandlerUrl;
    private DiscoveryClient discoveryClient;

    
    public RegistrationHandlerClientService(
    		@Value("${enablerLogic.checkForRegistrationHandlerInEureka:false}") boolean checkForRegistrationHandlerInEureka,
    		@Value("${enablerLogic.registrationHandlerUrl:http://localhost:8001}") String registrationHandlerUrl,
            DiscoveryClient discoveryClient) {
        this.checkForRegistrationHandlerInEureka = checkForRegistrationHandlerInEureka;
		this.registrationHandlerUrl = registrationHandlerUrl;
        this.discoveryClient = discoveryClient;
        mapper = new ObjectMapper();
    }
    
    private void chackAndWaitRegistrationHandler() {
        if(registrationHandlerChecked)
            return;
        
        if(!checkForRegistrationHandlerInEureka) {
            LOG.info("Don't need to wait for RegistrationHandler in Eureka");
            registrationHandlerChecked = true;
            LOG.debug("Using RegistrationHandler in URL: {}", registrationHandlerUrl);  
			createFeignClient();
            return;
        }
        
        // wait for discovery client
        List<ServiceInstance> si = discoveryClient.getInstances("RegistrationHandler");
        int counter = 0;
        while(si.isEmpty()) {
            if((counter % 10) == 0)
               LOG.debug("Waiting for RegistrationHandler in Eureka. counter={}", counter);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            si = discoveryClient.getInstances("RegistrationHandler");
            if(counter >= 3000) {
                throw new RuntimeException("Waiting maximal time of 5min for RegistartionHandler in Eureka");
            }
        }
        
        registrationHandlerUrl = si.get(0).getUri().toString();
        LOG.debug("Found RegistrationHandler in Eureka at {}", registrationHandlerUrl);  
		createFeignClient();
    }

	private void createFeignClient() {
		client = Feign.builder()
        		.encoder(new JacksonEncoder())
        		.decoder(new JacksonDecoder())
        		.logger(new Slf4jLogger(RegistrationHandlerClientService.class))
        		.logLevel(Level.FULL)
            .target(RegistrationHandlerClient.class, registrationHandlerUrl);
	}
    
    public CloudResource registerResource(CloudResource resource) {
        chackAndWaitRegistrationHandler();
        return client.addResource(resource);
    }

    public List<CloudResource> registerResources(List<CloudResource> resources) {
        chackAndWaitRegistrationHandler();
        return client.addResources(resources);
    }
    
// TODO test & implementation RDF registration
//    @RequestMapping(method = RequestMethod.POST, path = "/rdf-resources")
//    public ResponseEntity<?> registerRdfResources(@RequestBody RdfCloudResorceList resources);

    public CloudResource updateResource(CloudResource resource) {
        chackAndWaitRegistrationHandler();
        return client.updateResource(resource);
    }
    
    public List<CloudResource> updateResources(List<CloudResource> resources) {
        chackAndWaitRegistrationHandler();
        return client.updateResources(resources);
    }
    
    public CloudResource unregisterResource(String resourceInternalId) {
        chackAndWaitRegistrationHandler();
        return client.deleteResource(resourceInternalId);
    }
    
    public List<CloudResource> unregisterResources(List<String> resourceInternalIds) {
        chackAndWaitRegistrationHandler();
        return client.deleteResources(resourceInternalIds);
    }
}
