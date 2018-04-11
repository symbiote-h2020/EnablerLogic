package eu.h2020.symbiote.enablerlogic.messaging;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;

import eu.h2020.symbiote.client.ClientConstants;
import eu.h2020.symbiote.client.RegistrationHandlerClient;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.model.cim.FeatureOfInterest;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.model.cim.WGS84Location;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

@RunWith(SpringRunner.class)

@Import({RegistrationHandlerClientService.class, RegistrationHandlerClientServiceTests.TestConfiguration.class})
@TestPropertySource(locations = "classpath:integration.properties")
@SpringBootTest(properties = {
        "RegistrationHandler.ribbon.eureka.enabled=false",
        "enablerLogic.registrationHandlerUrl=http://localhost:9001"
        
    })
@AutoConfigureWireMock(port = 9001)
@DirtiesContext
public class RegistrationHandlerClientServiceTests {
    
    @Autowired
    private RegistrationHandlerClientService service;
    
    @Configuration
    public static class TestConfiguration {
        @Bean
        public DiscoveryClient discoveryClient() {
            return new DiscoveryClient() {
                
                @Override
                public List<String> getServices() {
                    
                    // TODO Auto-generated method stub
                    return null;
                }
                
                @Override
                public ServiceInstance getLocalServiceInstance() {
                    // TODO Auto-generated method stub
                    return null;
                }
                
                @Override
                public List<ServiceInstance> getInstances(String serviceId) {
                    // TODO Auto-generated method stub
                    return null;
                }
                
                @Override
                public String description() {
                    // TODO Auto-generated method stub
                    return null;
                }
            };
        }
    }
    
    @Test
    public void registerOneResource_shouldReturnRegisteredResource() throws Exception {
        //given
        CloudResource sendResource = createCloudResource1();

        CloudResource expetedResource = createCloudResource1();
        expetedResource.getResource().setId(expetedResource.getInternalId());
        
        wiremockStubPost(ClientConstants.RH_RESOURCE_PATH, sendResource, expetedResource);

        // when
        CloudResource registeredResource = service.registerResource(sendResource);

        //then
        assertThat(registeredResource).isEqualToComparingOnlyGivenFields(sendResource, "internalId");
        assertThat(registeredResource.getResource().getId()).isNotNull();
    }

    @Test
    public void registerAlreadyRegisteredResource_shouldReturnRegisteredResource() throws Exception {
        //given
        CloudResource sendResource = createCloudResource1();
        
        CloudResource expetedResource = createCloudResource1();
        expetedResource.getResource().setId(expetedResource.getInternalId());
        
        wiremockStubPost(ClientConstants.RH_RESOURCE_PATH, sendResource, expetedResource);
        
        // when
        CloudResource registeredResource = service.registerResource(sendResource);
        
        //then
        assertThat(registeredResource).isNotNull();
    }
    
    @Test
    public void registeringWrongResourceObject_shouldReturnNull() throws Exception {
        //given
        CloudResource sendResource = createCloudResource1();
        sendResource.setInternalId(null);
        
        wiremockStubPost(ClientConstants.RH_RESOURCE_PATH, sendResource, null);

        // when
        CloudResource registeredResource = service.registerResource(sendResource);

        //then
        assertThat(registeredResource).isNull();
    }
    
    @Test
    public void unregisteringResource_shouldReturnRegisteredResource() throws Exception {
        //given
        CloudResource expetedResource = createCloudResource1();
        expetedResource.getResource().setId(expetedResource.getInternalId());
        
        wiremockStubDelete(ClientConstants.RH_RESOURCE_PATH, "testId1", expetedResource);
        
        // when
        CloudResource unregisteredResource = service.unregisterResource("testId1");
        
        //then
        assertThat(unregisteredResource.getInternalId()).isEqualTo("testId1");
    }
    
    @Test
    public void unregisteringNotRegisteredResource_shouldReturnNull() throws Exception {
        //given
        wiremockStubDelete(ClientConstants.RH_RESOURCE_PATH, "testId1", null);
        
        // when
        CloudResource unregisteredResource = service.unregisterResource("testId1");
        
        //then
        assertThat(unregisteredResource).isNull();
    }
    
    @Test
    public void registerMoreResources_shouldReturnListOfRegisteredResources() throws Exception {
        //given
        List<CloudResource> sendResources = createResources();
        
        List<CloudResource> expetedResources = createResources();
        expetedResources.get(0).getResource().setId(expetedResources.get(0).getInternalId());
        expetedResources.get(1).getResource().setId(expetedResources.get(1).getInternalId());
        
        wiremockStubPost(ClientConstants.RH_RESOURCES_PATH, sendResources, expetedResources);

        // when
        List<CloudResource> registeredResources = service.registerResources(sendResources);

        //then
        assertThat(registeredResources)
            .hasSize(2)
            .extracting("internalId", "resource.id").contains(
                    tuple("testId1", "testId1"),
                    tuple("testId2", "testId2")
            );
    }

    @Test
    public void registerAlreadyRegisteredResources_shouldReturnListOfRegisteredResources() throws Exception {
        //given
        List<CloudResource> sendResources = createResources();
        
        List<CloudResource> expetedResources = createResources();
        expetedResources.get(0).getResource().setId(expetedResources.get(0).getInternalId());
        expetedResources.get(1).getResource().setId(expetedResources.get(1).getInternalId());
        
        wiremockStubPost(ClientConstants.RH_RESOURCES_PATH, sendResources, expetedResources);
        
        // when
        List<CloudResource> registeredResources = service.registerResources(sendResources);
        
        //then
        assertThat(registeredResources)
            .hasSize(2)
            .extracting("internalId", "resource.id").contains(
                    tuple("testId1", "testId1"),
                    tuple("testId2", "testId2")
            );
    }
    
    @Test
    public void registerWrongResource0_shouldReturnListOfRegisteredResources() throws Exception {
        //given
        List<CloudResource> sendResources = createResources();
        sendResources.get(0).setInternalId(null);
        
        List<CloudResource> expetedResources = Arrays.asList(createCloudResource2());
        expetedResources.get(0).getResource().setId(expetedResources.get(0).getInternalId());
        
        wiremockStubPost(ClientConstants.RH_RESOURCES_PATH, sendResources, expetedResources);
        
        
        // when
        List<CloudResource> registeredResources = service.registerResources(sendResources);
        
        //then
        assertThat(registeredResources)
            .hasSize(1)
            .extracting("internalId", "resource.id").contains(
                    tuple("testId2", "testId2")
            );
    }
    
    @Test
    public void unregisterResources_shouldReturnListOfUnregisteredResources() throws Exception {
        //given
        List<CloudResource> expectedResources = createResources();

        List<String> resourceIds = expectedResources.stream()
                .map(r -> r.getInternalId())
                .collect(Collectors.toList());
        
        wiremockStubDelete(ClientConstants.RH_RESOURCES_PATH, resourceIds, expectedResources);

        // when
        List<CloudResource> registeredResources = service.unregisterResources(resourceIds);
        
        //then
        assertThat(registeredResources)
            .hasSize(2)
            .extracting("internalId").contains("testId1", "testId2");
    }

    @Test
    public void unregisterResourcesThatAreNotRegistered_shouldReturnListOfUnregisteredResources() throws Exception {
        //given
        List<CloudResource> cloudResources = createResources();
        List<String> resourceIds = cloudResources.stream()
                .map(r -> r.getInternalId())
                .collect(Collectors.toList());
        
        wiremockStubDelete(ClientConstants.RH_RESOURCES_PATH, resourceIds, new LinkedList<>());

        // when
        List<CloudResource> registeredResources = service.unregisterResources(resourceIds);
        
        //then
        assertThat(registeredResources).isEmpty();
    }
    
    @Test
    public void updateResource_shouldReturnUpdatedResource() throws Exception {
        //given
        CloudResource cloudResource = createCloudResource1();
        cloudResource.setPluginId("new plugin id");
        
        wiremockStubPut(ClientConstants.RH_RESOURCE_PATH, cloudResource, cloudResource);
        
        // when
        CloudResource registeredResource = service.updateResource(cloudResource);
        
        //then
        assertThat(registeredResource.getInternalId()).isEqualTo("testId1");
        assertThat(registeredResource.getPluginId()).isEqualTo("new plugin id");
    }
    
    @Test
    public void updateResources_shouldReturnListOfUpdatedResources() throws Exception {
        //given
        List<CloudResource> cloudResources = createResources();
        cloudResources.get(0).setPluginId("new plugin id");

        wiremockStubPut(ClientConstants.RH_RESOURCES_PATH, cloudResources, cloudResources);
        
        // when
        List<CloudResource> registeredResources = service.updateResources(cloudResources);
        
        //then
        assertThat(registeredResources)
            .hasSize(2)
            .extracting("internalId", "pluginId").contains(
                tuple("testId1", "new plugin id"),
                tuple("testId2", "testPlugin")
            );
    }
    
    private void wiremockStubPost(String url, Object sendResource, Object expetedResource)
            throws IOException, JsonGenerationException, JsonMappingException {
        stubFor(post(urlEqualTo(url))
                .withRequestBody(resourceEqualToJson(sendResource))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(convertToJson(expetedResource))));
    }

    private void wiremockStubPut(String url, Object sendResource, Object expetedResource)
            throws IOException, JsonGenerationException, JsonMappingException {
        stubFor(put(urlEqualTo(url))
                .withRequestBody(resourceEqualToJson(sendResource))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(convertToJson(expetedResource))));
    }
    
    private void wiremockStubDelete(String url, String sendResource, Object expetedResource)
            throws IOException, JsonGenerationException, JsonMappingException {
        stubFor(delete(urlPathMatching(url))
                .withQueryParam("resourceInternalId", equalTo(sendResource))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(convertToJson(expetedResource))));
    }
    
    private void wiremockStubDelete(String url, List<String> sendResource, Object expetedResource)
            throws IOException, JsonGenerationException, JsonMappingException {
        stubFor(delete(urlPathMatching(url))
                //.withQueryParam("resourceInternalId", equalTo(String.join("%2C", sendResource)))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(convertToJson(expetedResource))));
    }
    
    private EqualToJsonPattern resourceEqualToJson(Object resource)
            throws IOException, JsonGenerationException, JsonMappingException {
        return new EqualToJsonPattern(convertToJson(resource), true, true);
    }

    private String convertToJson(Object resource)
            throws IOException, JsonGenerationException, JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, resource);
        String body = writer.toString();
        return body;
    }
    
    private List<CloudResource> createResources() {
        return Arrays.asList(createCloudResource1(), createCloudResource2());
    }
    
    private CloudResource createCloudResource2() {
        CloudResource r = createCloudResource1();
        r.setInternalId("testId2");
        return r;
    }
    
    private CloudResource createCloudResource1() {
        CloudResource resource = new CloudResource();
        resource.setInternalId("testId1");
        resource.setPluginId("testPlugin");
        StationarySensor sensor = new StationarySensor();
        resource.setResource(sensor);
        sensor.setName("lamp");
        sensor.setDescription(Arrays.asList("A comment"));
        sensor.setInterworkingServiceURL("https://symbiote-h2020.eu/example/interworkingService/");
        sensor.setLocatedAt(new WGS84Location(2.349014, 48.864716, 15, 
                "Paris", 
                Arrays.asList("This is Paris")));
        FeatureOfInterest featureOfInterest = new FeatureOfInterest();
        sensor.setFeatureOfInterest(featureOfInterest);
        featureOfInterest.setName("Room1");
        featureOfInterest.setDescription(Arrays.asList("This is room 1"));
        featureOfInterest.setHasProperty(Arrays.asList("temperature"));
        sensor.setObservesProperty(Arrays.asList("temperature,humidity".split(",")));
        return resource;
    }
}
