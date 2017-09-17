package eu.h2020.symbiote.enablerlogic.rap.plugin;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rabbitmq.client.Channel;

import eu.h2020.symbiote.cloud.model.data.Result;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.enablerlogic.messaging.RabbitManager;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.EmbeddedRabbitFixture;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.TestingRabbitConfig;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.PluginProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.RoutingKeysProperties;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.enablerlogic.rap.plugin.RapPlugin;
import eu.h2020.symbiote.enablerlogic.rap.resources.RapDefinitions;
import eu.h2020.symbiote.enablerlogic.rap.resources.db.ResourceInfo;

@RunWith(SpringRunner.class)
@Import({RabbitManager.class,
    TestingRabbitConfig.class,
    EnablerLogicProperties.class})
@EnableConfigurationProperties({RabbitConnectionProperties.class, ExchangeProperties.class, RoutingKeysProperties.class, PluginProperties.class})
@DirtiesContext
public class RapPluginAccessTest extends EmbeddedRabbitFixture {
    private static final Logger LOG = LoggerFactory.getLogger(RapPluginAccessTest.class);
    private static final String PLUGIN_REGISTRATION_EXCHANGE = RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_OUT;
    private static final String PLUGIN_EXCHANGE = RapDefinitions.PLUGIN_EXCHANGE_IN;
    private static final String RAP_QUEUE_NAME = "test_rap";
    
    private static final int RECEIVE_TIMEOUT = 20_000;
    
    @Configuration
    public static class TestConfiguration {
        @Bean
        public RapPlugin rapPlugin(RabbitManager manager) {
            return new RapPlugin(manager, "enablerName", false, true);
        }
    }
    
    @Autowired
    private RapPlugin rapPlugin;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ConnectionFactory factory;
    
    private Connection connection;
    private Channel channel;
    private ObjectMapper mapper;
    
    @Autowired
    private ApplicationContext ctx;
    
    @Before
    public void initialize() throws Exception {
        LOG.debug("All beans - names: {}", String.join(", ", ctx.getBeanDefinitionNames()));
        initializeJacksonMapper();
        initializeRabbitResources();
    }
    
    public void initializeJacksonMapper() {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    public void initializeRabbitResources() throws Exception {
        rabbitTemplate.setReceiveTimeout(RECEIVE_TIMEOUT);
        connection = factory.createConnection();
        channel = connection.createChannel(false);

        createRabbitResources();
    }
    
    private void createRabbitResources() throws IOException {
        rabbitTemplate.setReceiveTimeout(RECEIVE_TIMEOUT);
        channel.exchangeDeclare(PLUGIN_REGISTRATION_EXCHANGE, "topic", true, false, false, null);
        channel.exchangeDeclare(PLUGIN_EXCHANGE, "topic", true, false, false, null);
        channel.queueDeclare(RAP_QUEUE_NAME, true, false, false, null);
    }

    @After
    public void teardownRabbitResources() throws Exception {
        cleanRabbitResources();
    }

    private void cleanRabbitResources() throws IOException {
        channel.exchangeDelete(PLUGIN_REGISTRATION_EXCHANGE);
        channel.queueDelete(RAP_QUEUE_NAME);
        channel.exchangeDelete(PLUGIN_EXCHANGE);
    }

    @Test @DirtiesContext
    public void sendingResourceAccessGetMessage_whenExceptionInPlugin_shouldReturnEmptyList() throws Exception {
        //given
        ReadingResourceListener readingListener = Mockito.mock(ReadingResourceListener.class);
        when(readingListener.readResource(getInternalId())).thenThrow(new RuntimeException("exception message"));
        rapPlugin.registerReadingResourceListener(readingListener);
        
        List<ResourceInfo> infoList = Arrays.asList(new ResourceInfo(getSymbioteId(), getInternalId()));
        ResourceAccessGetMessage msg = new ResourceAccessGetMessage(infoList);
        String json = mapper.writeValueAsString(msg);
        
        String routingKey =  "enablerName.get";
    
        // when
        Object returnedObject = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, routingKey, json);          
    
        //then
        assertNotNull(returnedObject);
        assertThat(returnedObject).isInstanceOf(List.class);
        
        List<Observation> returnedObservations = (List<Observation>)returnedObject;
        assertThat(returnedObservations).isEmpty();
    }


    @Test @DirtiesContext
    public void sendingResourceAccessGetMessage_shouldReturnResourceReading() throws Exception {
        //given
        ReadingResourceListener readingListener = Mockito.mock(ReadingResourceListener.class);
        when(readingListener.readResource(getInternalId())).thenReturn(getExpectedObservation());
        rapPlugin.registerReadingResourceListener(readingListener);
        
        List<ResourceInfo> infoList = Arrays.asList(new ResourceInfo(getSymbioteId(), getInternalId()));
        ResourceAccessGetMessage msg = new ResourceAccessGetMessage(infoList);
        String json = mapper.writeValueAsString(msg);
        
        String routingKey =  "enablerName.get";
        Message sendMessage = rabbitTemplate.getMessageConverter().toMessage(json, new MessageProperties());
    
        // when
        Message receivedMessage = rabbitTemplate.sendAndReceive(PLUGIN_EXCHANGE, routingKey, sendMessage);    
        
    
        //then
        assertThat(receivedMessage).isNotNull();

        List<Observation> returnedObservations = new ObjectMapper().readValue(receivedMessage.getBody(), new TypeReference<List<Observation>>() { });
        assertThat(returnedObservations)
            .hasSize(1)
            .extracting(Observation::getResourceId)
                .contains("platformResourceId");
    }

    @Test @DirtiesContext
    public void sendingResourceAccessHistoryMessage_whenExceptionInPlugin_shouldReturnHistoryOfResourceReading() throws Exception {
        //given
        ReadingResourceListener readingListener = Mockito.mock(ReadingResourceListener.class);
        when(readingListener.readResourceHistory(getInternalId())).thenThrow(new RuntimeException("excaption message"));
        rapPlugin.registerReadingResourceListener(readingListener);
        
        List<ResourceInfo> infoList = Arrays.asList(new ResourceInfo(getSymbioteId(), getInternalId()));
        ResourceAccessHistoryMessage msg = new ResourceAccessHistoryMessage(infoList, 10, null);
        String json = mapper.writeValueAsString(msg);
        
        String routingKey =  "enablerName.history";
    
        // when
        Object returnedObject = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, routingKey, json);          
    
        //then
        assertNotNull(returnedObject);
        assertThat(returnedObject).isInstanceOf(List.class);
        
        List<Observation> returnedObservations = (List<Observation>) returnedObject;
        assertThat(returnedObservations).isEmpty();
    }

    @Test @DirtiesContext
    public void sendingResourceAccessHistoryMessage_shouldReturnHistoryOfResourceReading() throws Exception {
        //given
        ReadingResourceListener readingListener = Mockito.mock(ReadingResourceListener.class);
        when(readingListener.readResourceHistory(getInternalId())).thenReturn(getExpectedObservations());
        rapPlugin.registerReadingResourceListener(readingListener);
        
        List<ResourceInfo> infoList = Arrays.asList(new ResourceInfo(getSymbioteId(), getInternalId()));
        ResourceAccessHistoryMessage msg = new ResourceAccessHistoryMessage(infoList, 10, null);
        String json = mapper.writeValueAsString(msg);
        
        String routingKey =  "enablerName.history";
        Message sendMessage = rabbitTemplate.getMessageConverter().toMessage(json, new MessageProperties());
        
        // when
        Message receivedMessage = rabbitTemplate.sendAndReceive(PLUGIN_EXCHANGE, routingKey, sendMessage);          
    
        //then
        assertNotNull(receivedMessage);
        
        List<Observation> returnedObservations = mapper.readValue(receivedMessage.getBody(), 
                new TypeReference<List<Observation>>() { });
        assertThat(returnedObservations)
            .hasSize(2)
            .extracting(Observation::getResourceId)
                .contains("platformResourceId", "platformResourceId");
    }

    @Test @DirtiesContext
    public void sendingResourceAccessSetMessageForActuation_whenExceptionInPlugin_shouldReturnNull() throws Exception {
        //given
        WritingToResourceListener writingListener = Mockito.mock(WritingToResourceListener.class);
        
        List<InputParameter> parameterList = Arrays.asList(newInputParameter("name1", "value1"),
                newInputParameter("name2", "value2"));
        when(writingListener.writeResource(getInternalId(), parameterList)).thenThrow(new RuntimeException("exception message"));
        rapPlugin.registerWritingToResourceListener(writingListener);
        
        List<ResourceInfo> infoList = Arrays.asList(new ResourceInfo(getSymbioteId(), getInternalId()));
        String body = mapper.writeValueAsString(parameterList);
        ResourceAccessSetMessage msg = new ResourceAccessSetMessage(infoList, body);
        String json = mapper.writeValueAsString(msg);
        
        String routingKey =  "enablerName.set";
    
        // when
        Object returnedJson = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, routingKey, json);          
    
        //then
        assertNull(returnedJson);
    }

    @Test @DirtiesContext
    public void sendingResourceAccessSetMessageForService_whenExceptionInPlugin_shouldReturnNull() throws Exception {
        //given
        WritingToResourceListener writingListener = Mockito.mock(WritingToResourceListener.class);
        
        List<InputParameter> parameterList = Arrays.asList(newInputParameter("name1", "value1"),
                newInputParameter("name2", "value2"));
        when(writingListener.writeResource(getInternalId(), parameterList)).thenThrow(new RuntimeException("Exception message"));
        rapPlugin.registerWritingToResourceListener(writingListener);
        
        List<ResourceInfo> infoList = Arrays.asList(new ResourceInfo(getSymbioteId(), getInternalId()));
        String body = mapper.writeValueAsString(parameterList);
        ResourceAccessSetMessage msg = new ResourceAccessSetMessage(infoList, body);
        String json = mapper.writeValueAsString(msg);
        
        String routingKey =  "enablerName.set";
    
        // when
        Object returnedJson = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, routingKey, json);          
    
        //then
        assertThat(returnedJson).isNull();
//            .isNotNull()
//            .isInstanceOf(Result.class);
        
//        Result result = (Result) returnedJson;
//        assertThat(result.getValue()).isNull();
    }

    @Test @DirtiesContext
    public void sendingResourceAccessSetMessageForService_shouldReturnResult() throws Exception {
        //given
        WritingToResourceListener writingListener = Mockito.mock(WritingToResourceListener.class);
        
        when(writingListener.writeResource(eq(getInternalId()), anyList())).thenReturn(new Result<>(false, null, "result"));
        rapPlugin.registerWritingToResourceListener(writingListener);
        
        List<ResourceInfo> infoList = Arrays.asList(new ResourceInfo(getSymbioteId(), getInternalId()));
        List<InputParameter> parameterList = Arrays.asList(newInputParameter("name1", "value1"),
                newInputParameter("name2", "value2"));
        String body = mapper.writeValueAsString(parameterList);
        ResourceAccessSetMessage msg = new ResourceAccessSetMessage(infoList, body);
        String json = mapper.writeValueAsString(msg);
        
        String routingKey =  "enablerName.set";
        
        Message sendMessage = rabbitTemplate.getMessageConverter().toMessage(json, new MessageProperties());
    
        // when
        Message receivedMessage = rabbitTemplate.sendAndReceive(PLUGIN_EXCHANGE, routingKey, sendMessage);          
    
        //then
        assertThat(receivedMessage).isNotNull();

        Result<String> returnedResult = mapper.readValue(receivedMessage.getBody(), 
                new TypeReference<Result<String>>() { });
        assertThat(returnedResult.getValue()).isEqualTo("result");
    }

    private InputParameter newInputParameter(String name, String value) {
        InputParameter parameter = new InputParameter(name);
        parameter.setValue(value);
        return parameter;
    }

    private List<Observation> getExpectedObservation() {
        List<Observation> observations = new LinkedList<>();
        observations.add(newObservation());
        return observations;
    }

    private List<Observation> getExpectedObservations() {
        List<Observation> observations = new LinkedList<>();
        observations.add(newObservation());
        observations.add(newObservation());
        return observations;
    }
    private Observation newObservation() {
        return new Observation(getInternalId(), null, null, null, null);
    }

    private String getSymbioteId() {
        return "resourceId";
    }

    private String getInternalId() {
        return "platformResourceId";
    }
}