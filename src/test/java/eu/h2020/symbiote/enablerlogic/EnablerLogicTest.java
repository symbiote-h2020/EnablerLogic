package eu.h2020.symbiote.enablerlogic;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.enabler.messaging.model.ActuatorExecutionTaskInfo;
import eu.h2020.symbiote.enabler.messaging.model.CancelTaskRequest;
import eu.h2020.symbiote.enabler.messaging.model.CancelTaskResponse;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyTaskInfo;
import eu.h2020.symbiote.enabler.messaging.model.ProblematicResourcesMessage;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerUpdateRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerUpdateResponse;
import eu.h2020.symbiote.enabler.messaging.model.ServiceExecutionTaskInfo;
import eu.h2020.symbiote.enabler.messaging.model.ServiceExecutionTaskResponse;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.messaging.RabbitManager;
import eu.h2020.symbiote.enablerlogic.messaging.VoidResponse;
import eu.h2020.symbiote.enablerlogic.messaging.WrongResponseException;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.AsyncMessageFromEnablerLogicConsumer;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.SyncMessageFromEnablerLogicConsumer;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;

@RunWith(MockitoJUnitRunner.class)
public class EnablerLogicTest {
    private EnablerLogicProperties props;
    private EnablerLogic enablerLogic;

    @Mock
    private RabbitManager rabbitManager;


    @Before
    public void setup() {
        props = new EnablerLogicProperties();
        enablerLogic = new EnablerLogic(rabbitManager, props);
    }
    
    @Test
	public void nullParameterInConstructor_throwsException() throws Exception {
    	assertThatIllegalStateException().isThrownBy(() -> {
    		new EnablerLogic(null, null);
    	}).withMessage("Props may not be null");
	}
    
    @Test
    public void queryResourceManagerWithOneRequest_shouldReturnResponse() throws Exception {
        // given
        ArgumentCaptor<ResourceManagerAcquisitionStartRequest> captor =
            ArgumentCaptor.forClass(ResourceManagerAcquisitionStartRequest.class);
        ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest("id", 1, 1, new CoreQueryRequest(), "P0-0-0T0:0:0.06", false, "P0-0-0T0:0:1", false, "elName", null);
        ResourceManagerAcquisitionStartResponse expectedResponse = new ResourceManagerAcquisitionStartResponse();
        String exchangeName = props.getExchange().getResourceManager().getName();
		String routingKey = props.getKey().getResourceManager().getStartDataAcquisition();
		when(rabbitManager.sendRpcMessage(
            eq(exchangeName),
            eq(routingKey),
            any(ResourceManagerAcquisitionStartRequest.class))).thenReturn(expectedResponse);

        // when
        ResourceManagerAcquisitionStartResponse response = enablerLogic.queryResourceManager(request);

        // then
        verify(rabbitManager).sendRpcMessage(eq(exchangeName),
            eq(routingKey),
            captor.capture());
        ResourceManagerAcquisitionStartRequest requests = captor.getValue();
        assertThat(requests.getTasks()).hasSize(1);
        assertThat(requests.getTasks()).contains(request);
        assertThat(response).isSameAs(expectedResponse);
    }

    @Test
    public void queryResourceManagerWithOneRequest_onTimeout_ShouldReturnNull() throws Exception {
    	// given
    	ArgumentCaptor<ResourceManagerAcquisitionStartRequest> captor =
    			ArgumentCaptor.forClass(ResourceManagerAcquisitionStartRequest.class);
    	ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest("id", 1, 1, new CoreQueryRequest(), "P0-0-0T0:0:0.06", false, "P0-0-0T0:0:1", false, "elName", null);
    	String exchangeName = props.getExchange().getResourceManager().getName();
    	String routingKey = props.getKey().getResourceManager().getStartDataAcquisition();
    	when(rabbitManager.sendRpcMessage(
    			eq(exchangeName),
    			eq(routingKey),
    			any(ResourceManagerAcquisitionStartRequest.class))).thenReturn(null);
    	
    	// when
    	ResourceManagerAcquisitionStartResponse response = enablerLogic.queryResourceManager(request);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(exchangeName),
    			eq(routingKey),
    			captor.capture());
    	ResourceManagerAcquisitionStartRequest requests = captor.getValue();
    	assertThat(requests.getTasks()).hasSize(1);
    	assertThat(requests.getTasks()).contains(request);
    	assertThat(response).isEqualTo(null);
    }
    
    @Test
    public void queryResourceManagerWithOneRequestAndTimeout_shouldReturnResponse() throws Exception {
        // given
        ArgumentCaptor<ResourceManagerAcquisitionStartRequest> captor =
            ArgumentCaptor.forClass(ResourceManagerAcquisitionStartRequest.class);
        ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest("id", 1, 1, new CoreQueryRequest(), "P0-0-0T0:0:0.06", false, "P0-0-0T0:0:1", false, "elName", null);
        ResourceManagerAcquisitionStartResponse expectedResponse = new ResourceManagerAcquisitionStartResponse();
        String exchangeName = props.getExchange().getResourceManager().getName();
		String routingKey = props.getKey().getResourceManager().getStartDataAcquisition();
		when(rabbitManager.sendRpcMessage(
            eq(exchangeName),
            eq(routingKey),
            any(ResourceManagerAcquisitionStartRequest.class),
            eq(1000))).thenReturn(expectedResponse);

        // when
        ResourceManagerAcquisitionStartResponse response = enablerLogic.queryResourceManager(1000, request);

        // then
        verify(rabbitManager).sendRpcMessage(eq(exchangeName),
            eq(routingKey),
            captor.capture(),
            eq(1000));
        ResourceManagerAcquisitionStartRequest requests = captor.getValue();
        assertThat(requests.getTasks()).hasSize(1);
        assertThat(requests.getTasks()).contains(request);
        assertThat(response).isSameAs(expectedResponse);
    }

    @Test
    public void cancelTask_shouldReturnCancellationResponse() throws Exception {
        // given
        ArgumentCaptor<CancelTaskRequest> captor =
                ArgumentCaptor.forClass(CancelTaskRequest.class);
        CancelTaskRequest expectedRequest = new CancelTaskRequest();
        CancelTaskResponse expectedResponse = new CancelTaskResponse();
        String exchangeName = props.getExchange().getResourceManager().getName();
		String routingKey = props.getKey().getResourceManager().getCancelTask();
		when(rabbitManager.sendRpcMessage(
                eq(exchangeName),
                eq(routingKey),
                any(ResourceManagerAcquisitionStartRequest.class))).thenReturn(expectedResponse);
        
        // when
        CancelTaskResponse response = enablerLogic.cancelTask(expectedRequest);
        
        // then
        verify(rabbitManager).sendRpcMessage(eq(exchangeName),
                eq(routingKey),
                captor.capture());
        CancelTaskRequest request = captor.getValue();
        assertThat(request).isSameAs(expectedRequest);
        assertThat(response).isEqualTo(expectedResponse);
    }
    
    @Test
    public void cancelTask_onTimeout_shouldReturnNull() throws Exception {
    	// given
    	ArgumentCaptor<CancelTaskRequest> captor =
    			ArgumentCaptor.forClass(CancelTaskRequest.class);
    	CancelTaskRequest expectedRequest = new CancelTaskRequest();
    	String exchangeName = props.getExchange().getResourceManager().getName();
    	String routingKey = props.getKey().getResourceManager().getCancelTask();
    	when(rabbitManager.sendRpcMessage(
    			eq(exchangeName),
    			eq(routingKey),
    			any(ResourceManagerAcquisitionStartRequest.class))).thenReturn(null);
    	
    	// when
    	CancelTaskResponse response = enablerLogic.cancelTask(expectedRequest);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(exchangeName),
    			eq(routingKey),
    			captor.capture());
    	CancelTaskRequest request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(response).isEqualTo(null);
    }
    
    @Test
    public void cancelTaskWithTimeout_onTimeout_shouldReturnNull() throws Exception {
    	// given
    	ArgumentCaptor<CancelTaskRequest> captor =
    			ArgumentCaptor.forClass(CancelTaskRequest.class);
    	CancelTaskRequest expectedRequest = new CancelTaskRequest();
    	String exchangeName = props.getExchange().getResourceManager().getName();
    	String routingKey = props.getKey().getResourceManager().getCancelTask();
    	when(rabbitManager.sendRpcMessage(
    			eq(exchangeName),
    			eq(routingKey),
    			any(ResourceManagerAcquisitionStartRequest.class),
    			eq(1000))).thenReturn(null);
    	
    	// when
    	CancelTaskResponse response = enablerLogic.cancelTask(expectedRequest, 1000);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(exchangeName),
    			eq(routingKey),
    			captor.capture(),
    			eq(1000));
    	CancelTaskRequest request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(response).isEqualTo(null);
    }
    
    @Test
    public void updateTask_shouldReturnUpdateResponse() throws Exception {
        // given
        ArgumentCaptor<ResourceManagerUpdateRequest> captor =
                ArgumentCaptor.forClass(ResourceManagerUpdateRequest.class);
        ResourceManagerUpdateRequest expectedRequest = new ResourceManagerUpdateRequest();
        ResourceManagerUpdateResponse expectedResponse = new ResourceManagerUpdateResponse();
        String exchangeName = props.getExchange().getResourceManager().getName();
		String routingKey = props.getKey().getResourceManager().getUpdateTask();
		when(rabbitManager.sendRpcMessage(
                eq(exchangeName),
                eq(routingKey),
                any(ResourceManagerUpdateRequest.class))).thenReturn(expectedResponse);
        
        // when
        ResourceManagerUpdateResponse response = enablerLogic.updateTask(expectedRequest);
        
        // then
        verify(rabbitManager).sendRpcMessage(eq(exchangeName),
                eq(props.getKey().getResourceManager().getUpdateTask()),
                captor.capture());
        ResourceManagerUpdateRequest request = captor.getValue();
        assertThat(request).isSameAs(expectedRequest);
        assertThat(response).isSameAs(expectedResponse);
    }    

    @Test
    public void updateTask_shouldReturnNullIfTimeout() throws Exception {
    	// given
    	ArgumentCaptor<ResourceManagerUpdateRequest> captor =
    			ArgumentCaptor.forClass(ResourceManagerUpdateRequest.class);
    	ResourceManagerUpdateRequest expectedRequest = new ResourceManagerUpdateRequest();
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getResourceManager().getName()),
    			eq(props.getKey().getResourceManager().getCancelTask()),
    			any(ResourceManagerUpdateRequest.class),
    			eq(1000))).thenReturn(null);
    	
    	// when
    	ResourceManagerUpdateResponse response = enablerLogic.updateTask(expectedRequest, 1000);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getResourceManager().getName()),
    			eq(props.getKey().getResourceManager().getUpdateTask()),
    			captor.capture(),
    			eq(1000));
    	ResourceManagerUpdateRequest request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(response).isNull();
    }    
    
    @Test
    public void reportBrokenResource_shouldCallRabbitManager() throws Exception {
        // given
        ProblematicResourcesMessage message = new ProblematicResourcesMessage();
        
        // when
        enablerLogic.reportBrokenResource(message);

        // then
        verify(rabbitManager).sendMessage(
                eq(props.getExchange().getResourceManager().getName()),
                eq(props.getKey().getResourceManager().getWrongData()),
                eq((Object) message));
    }
    
    @Test
    public void readResource_shouldReturnResponse() throws Exception {
        // given
        ArgumentCaptor<PlatformProxyTaskInfo> captor =
            ArgumentCaptor.forClass(PlatformProxyTaskInfo.class);
        PlatformProxyTaskInfo expectedRequest = new PlatformProxyTaskInfo();
        EnablerLogicDataAppearedMessage response = new EnablerLogicDataAppearedMessage();
        when(rabbitManager.sendRpcMessage(
            eq(props.getExchange().getEnablerPlatformProxy().getName()),
            eq(props.getKey().getEnablerPlatformProxy().getSingleReadRequested()),
            any(PlatformProxyTaskInfo.class))).thenReturn(response);

        // when
        EnablerLogicDataAppearedMessage readResponse = enablerLogic.readResource(expectedRequest);

        // then
        verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
            eq(props.getKey().getEnablerPlatformProxy().getSingleReadRequested()),
            captor.capture());
        PlatformProxyTaskInfo request = captor.getValue();
        assertThat(request).isSameAs(expectedRequest);
        assertThat(readResponse).isSameAs(response);
    }

    @Test
    public void readResource_onTimeout_shouldReturnNull() throws Exception {
    	// given
    	ArgumentCaptor<PlatformProxyTaskInfo> captor =
    			ArgumentCaptor.forClass(PlatformProxyTaskInfo.class);
    	PlatformProxyTaskInfo expectedRequest = new PlatformProxyTaskInfo();
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getSingleReadRequested()),
    			any(PlatformProxyTaskInfo.class))).thenReturn(null);
    	
    	// when
    	EnablerLogicDataAppearedMessage readResponse = enablerLogic.readResource(expectedRequest);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getSingleReadRequested()),
    			captor.capture());
    	PlatformProxyTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(readResponse).isNull();
    }
    
    @Test
    public void readResourceWithTimeout_shouldReturnResponse() throws Exception {
        // given
        ArgumentCaptor<PlatformProxyTaskInfo> captor =
            ArgumentCaptor.forClass(PlatformProxyTaskInfo.class);
        PlatformProxyTaskInfo expectedRequest = new PlatformProxyTaskInfo();
        EnablerLogicDataAppearedMessage response = new EnablerLogicDataAppearedMessage();
        when(rabbitManager.sendRpcMessage(
            eq(props.getExchange().getEnablerPlatformProxy().getName()),
            eq(props.getKey().getEnablerPlatformProxy().getSingleReadRequested()),
            any(PlatformProxyTaskInfo.class),
            eq(1000))).thenReturn(response);

        // when
        EnablerLogicDataAppearedMessage readResponse = enablerLogic.readResource(expectedRequest, 1000);

        // then
        verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
            eq(props.getKey().getEnablerPlatformProxy().getSingleReadRequested()),
            captor.capture(),
            eq(1000));
        PlatformProxyTaskInfo request = captor.getValue();
        assertThat(request).isSameAs(expectedRequest);
        assertThat(readResponse).isSameAs(response);
    }

    @Test
    public void readResourceWithTimeout_onTimeout_shouldReturnNull() throws Exception {
    	// given
    	ArgumentCaptor<PlatformProxyTaskInfo> captor =
    			ArgumentCaptor.forClass(PlatformProxyTaskInfo.class);
    	PlatformProxyTaskInfo expectedRequest = new PlatformProxyTaskInfo();
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getSingleReadRequested()),
    			any(PlatformProxyTaskInfo.class),
    			eq(1000))).thenReturn(null);
    	
    	// when
    	EnablerLogicDataAppearedMessage readResponse = enablerLogic.readResource(expectedRequest, 1000);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getSingleReadRequested()),
    			captor.capture(),
    			eq(1000));
    	PlatformProxyTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(readResponse).isNull();
    }
    
    @Test
    public void triggeringActuator_shouldReturnResponse() throws Exception {
    	// given
    	ArgumentCaptor<ActuatorExecutionTaskInfo> captor =
    			ArgumentCaptor.forClass(ActuatorExecutionTaskInfo.class);
    	ActuatorExecutionTaskInfo expectedRequest = new ActuatorExecutionTaskInfo(
    			"taskId", null, props.getExchange().getEnablerPlatformProxy().getName(), 
    			"OnFooCapability", null);
    	ServiceExecutionTaskResponse response = new ServiceExecutionTaskResponse(HttpStatus.OK, "");
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteActuatorRequested()),
    			any(PlatformProxyTaskInfo.class))).thenReturn(response);
    	
    	// when
    	ServiceExecutionTaskResponse actuationResponse = enablerLogic.triggerActuator(expectedRequest);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteActuatorRequested()),
    			captor.capture());
    	ActuatorExecutionTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(actuationResponse).isSameAs(response);
    }
    
    @Test
    public void triggeringActuator_onTimeout_shouldReturnNull() throws Exception {
    	// given
    	ArgumentCaptor<ActuatorExecutionTaskInfo> captor =
    			ArgumentCaptor.forClass(ActuatorExecutionTaskInfo.class);
    	ActuatorExecutionTaskInfo expectedRequest = new ActuatorExecutionTaskInfo(
    			"taskId", null, props.getExchange().getEnablerPlatformProxy().getName(), 
    			"OnOffCapability", null);
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteActuatorRequested()),
    			any(PlatformProxyTaskInfo.class))).thenReturn(null);
    	
    	// when
    	ServiceExecutionTaskResponse actuationResponse = enablerLogic.triggerActuator(expectedRequest);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteActuatorRequested()),
    			captor.capture());
    	ActuatorExecutionTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(actuationResponse).isNull();
    }
    
    @Test
    public void triggeringActuatorWithTimeout_shouldReturnResponse() throws Exception {
    	// given
    	ArgumentCaptor<ActuatorExecutionTaskInfo> captor =
    			ArgumentCaptor.forClass(ActuatorExecutionTaskInfo.class);
    	ActuatorExecutionTaskInfo expectedRequest = new ActuatorExecutionTaskInfo(
    			"taskId", null, props.getExchange().getEnablerPlatformProxy().getName(), 
    			"OnFooCapability", null);
    	ServiceExecutionTaskResponse response = new ServiceExecutionTaskResponse(HttpStatus.OK, "");
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteActuatorRequested()),
    			any(PlatformProxyTaskInfo.class),
    			eq(1000))).thenReturn(response);
    	
    	// when
    	ServiceExecutionTaskResponse actuationResponse = enablerLogic.triggerActuator(expectedRequest, 1000);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteActuatorRequested()),
    			captor.capture(),
    			eq(1000));
    	ActuatorExecutionTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(actuationResponse).isSameAs(response);
    }
    
    @Test
    public void triggeringActuatorWithTimeout_onTimeout_shouldReturnNull() throws Exception {
    	// given
    	ArgumentCaptor<ActuatorExecutionTaskInfo> captor =
    			ArgumentCaptor.forClass(ActuatorExecutionTaskInfo.class);
    	ActuatorExecutionTaskInfo expectedRequest = new ActuatorExecutionTaskInfo(
    			"taskId", null, props.getExchange().getEnablerPlatformProxy().getName(), 
    			"OnOffCapability", null);
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteActuatorRequested()),
    			any(PlatformProxyTaskInfo.class),
    			eq(1000))).thenReturn(null);
    	
    	// when
    	ServiceExecutionTaskResponse actuationResponse = enablerLogic.triggerActuator(expectedRequest, 1000);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteActuatorRequested()),
    			captor.capture(),
    			eq(1000));
    	ActuatorExecutionTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(actuationResponse).isNull();
    }
    
    @Test
    public void invokingService_shouldReturnResponse() throws Exception {
    	// given
    	ArgumentCaptor<ServiceExecutionTaskInfo> captor =
    			ArgumentCaptor.forClass(ServiceExecutionTaskInfo.class);
    	ServiceExecutionTaskInfo expectedRequest = new ServiceExecutionTaskInfo(
    			"taskId", null, props.getExchange().getEnablerPlatformProxy().getName(), 
    			null);
    	ServiceExecutionTaskResponse response = new ServiceExecutionTaskResponse(HttpStatus.OK, "result");
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteServiceRequested()),
    			any(PlatformProxyTaskInfo.class))).thenReturn(response);
    	
    	// when
    	ServiceExecutionTaskResponse actuationResponse = enablerLogic.invokeService(expectedRequest);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteServiceRequested()),
    			captor.capture());
    	ServiceExecutionTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(actuationResponse).isSameAs(response);
    }
    
    @Test
    public void invokingService_onTimeout_shouldReturnNull() throws Exception {
    	// given
    	ArgumentCaptor<ServiceExecutionTaskInfo> captor =
    			ArgumentCaptor.forClass(ServiceExecutionTaskInfo.class);
    	ServiceExecutionTaskInfo expectedRequest = new ServiceExecutionTaskInfo(
    			"taskId", null, props.getExchange().getEnablerPlatformProxy().getName(), 
    			null);
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteServiceRequested()),
    			any(PlatformProxyTaskInfo.class))).thenReturn(null);
    	
    	// when
    	ServiceExecutionTaskResponse actuationResponse = enablerLogic.invokeService(expectedRequest);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteServiceRequested()),
    			captor.capture());
    	ServiceExecutionTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(actuationResponse).isNull();
    }
    
    @Test
    public void invokingServiceWithTimeout_shouldReturnResponse() throws Exception {
    	// given
    	ArgumentCaptor<ServiceExecutionTaskInfo> captor =
    			ArgumentCaptor.forClass(ServiceExecutionTaskInfo.class);
    	ServiceExecutionTaskInfo expectedRequest = new ServiceExecutionTaskInfo(
    			"taskId", null, props.getExchange().getEnablerPlatformProxy().getName(), 
    			null);
    	ServiceExecutionTaskResponse response = new ServiceExecutionTaskResponse(HttpStatus.OK, "result");
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteServiceRequested()),
    			any(PlatformProxyTaskInfo.class),
    			eq(1000))).thenReturn(response);
    	
    	// when
    	ServiceExecutionTaskResponse actuationResponse = enablerLogic.invokeService(expectedRequest, 1000);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteServiceRequested()),
    			captor.capture(),
    			eq(1000));
    	ServiceExecutionTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(actuationResponse).isSameAs(response);
    }
    
    @Test
    public void invokingServiceWitTimeout_onTimeout_shouldReturnNull() throws Exception {
    	// given
    	ArgumentCaptor<ServiceExecutionTaskInfo> captor =
    			ArgumentCaptor.forClass(ServiceExecutionTaskInfo.class);
    	ServiceExecutionTaskInfo expectedRequest = new ServiceExecutionTaskInfo(
    			"taskId", null, props.getExchange().getEnablerPlatformProxy().getName(), 
    			null);
    	when(rabbitManager.sendRpcMessage(
    			eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteServiceRequested()),
    			any(PlatformProxyTaskInfo.class),
    			eq(1000))).thenReturn(null);
    	
    	// when
    	ServiceExecutionTaskResponse actuationResponse = enablerLogic.invokeService(expectedRequest, 1000);
    	
    	// then
    	verify(rabbitManager).sendRpcMessage(eq(props.getExchange().getEnablerPlatformProxy().getName()),
    			eq(props.getKey().getEnablerPlatformProxy().getExecuteServiceRequested()),
    			captor.capture(),
    			eq(1000));
    	ServiceExecutionTaskInfo request = captor.getValue();
    	assertThat(request).isSameAs(expectedRequest);
    	assertThat(actuationResponse).isNull();
    }

    @Test
    public void registerAsyncMessageFromEnablerLogicConsumer_shouldDelegateItToConsumer() throws Exception {
        //given
        Consumer<String> lambda = (m) -> { };

        AsyncMessageFromEnablerLogicConsumer asyncConsumer = Mockito.mock(AsyncMessageFromEnablerLogicConsumer.class);
        enablerLogic.setAsyncConsumer(asyncConsumer);

        // when
        enablerLogic.registerAsyncMessageFromEnablerLogicConsumer(String.class, lambda);

        //then
        verify(asyncConsumer).registerReceiver(String.class, lambda);
    }

    @Test
    public void unregisterAsyncMessageFromEnablerLogicConsumer_shouldDelegateItToConsumer() throws Exception {
        //given
        AsyncMessageFromEnablerLogicConsumer asyncConsumer = Mockito.mock(AsyncMessageFromEnablerLogicConsumer.class);
        enablerLogic.setAsyncConsumer(asyncConsumer);

        // when
        enablerLogic.unregisterAsyncMessageFromEnablerLogicConsumer(String.class);

        //then
        verify(asyncConsumer).unregisterReceiver(String.class);
    }

    @Test
    public void sendingAsyncMessageToEnablerLogic_shouldCallRabbitManager() throws Exception {
        // given
        String message = "test message";
        String enablerName = "destEnablerName";

        // when
        enablerLogic.sendAsyncMessageToEnablerLogic(enablerName, message);

        // then
        verify(rabbitManager).sendMessage(eq("symbIoTe.enablerLogic"),
            eq("symbIoTe.enablerLogic.asyncMessageToEnablerLogic.destEnablerName"),
            eq((Object) message));
    }

    @AllArgsConstructor
    public static class ReceivedMessage {
        @Getter
        private String receivedText;
    }

    @Test
    public void sendingSyncMessageToEnablerLogic_shouldCallRabbitManagerAndReturnResponse() throws Exception {
        // given
        String sendMessage = "test message";
        String enablerName = "destEnablerName";
        ReceivedMessage mockReceiveMessage = new ReceivedMessage("received message");

        when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
            "symbIoTe.enablerLogic.syncMessageToEnablerLogic.destEnablerName",
            (Object) sendMessage)).thenReturn(mockReceiveMessage);

        // when
        ReceivedMessage receivedMessage = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, ReceivedMessage.class);

        // then
        assertThat(receivedMessage).isSameAs(mockReceiveMessage);
    }

    @Test
    public void sendingSyncMessageToEnablerLogicWithTimeout_shouldCallRabbitManagerAndReturnResponse() throws Exception {
    	// given
    	String sendMessage = "test message";
    	String enablerName = "destEnablerName";
    	ReceivedMessage mockReceiveMessage = new ReceivedMessage("received message");
    	
    	when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
    			"symbIoTe.enablerLogic.syncMessageToEnablerLogic.destEnablerName",
    			(Object) sendMessage,
    			1000)).thenReturn(mockReceiveMessage);
    	
    	// when
    	ReceivedMessage receivedMessage = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, ReceivedMessage.class, 1000);
    	
    	// then
    	assertThat(receivedMessage).isSameAs(mockReceiveMessage);
    }
    
    @Test
    public void sendingSyncMessageToEnablerLogicAndExpectingVoid_shouldCallRabbitManagerAndReturnVoidResponse() throws Exception {
        // given
        String sendMessage = "test message";
        String enablerName = "destEnablerName";
        VoidResponse mockReceiveMessage = new VoidResponse();

        when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
            "symbIoTe.enablerLogic.syncMessageToEnablerLogic.destEnablerName",
            (Object) sendMessage)).thenReturn(mockReceiveMessage);

        // when
        VoidResponse receivedMessage = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, VoidResponse.class);

        // then
        assertThat(receivedMessage).isSameAs(mockReceiveMessage);
    }

    @Test
    public void sendingSyncMessageWithTimeoutToEnablerLogicAndExpectingVoid_shouldCallRabbitManagerAndReturnVoidResponse() throws Exception {
    	// given
    	String sendMessage = "test message";
    	String enablerName = "destEnablerName";
    	VoidResponse mockReceiveMessage = new VoidResponse();
    	
    	when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
    			"symbIoTe.enablerLogic.syncMessageToEnablerLogic.destEnablerName",
    			(Object) sendMessage,
    			1000)).thenReturn(mockReceiveMessage);
    	
    	// when
    	VoidResponse receivedMessage = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, VoidResponse.class, 1000);
    	
    	// then
    	assertThat(receivedMessage).isSameAs(mockReceiveMessage);
    }
    
    @Test
    public void sendingSyncMessageToEnablerLogic_whenTimeout_shouldCallRabbitManagerAndReturnNull() throws Exception {
        // given
        String sendMessage = "test message";
        String enablerName = "enabler name";

        when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
            "symbIoTe.enablerLogic.syncMessageToEnablerLogic.DefaultEnablerName",
            (Object) sendMessage)).thenReturn(null);

        // when
        ReceivedMessage receivedMessage = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, ReceivedMessage.class);

        // then
        assertThat(receivedMessage).isNull();
    }

    @Test
    public void sendingSyncMessageToEnablerLogicWithTimeout_whenTimeout_shouldCallRabbitManagerAndReturnNull() throws Exception {
    	// given
    	String sendMessage = "test message";
    	String enablerName = "enabler name";
    	
    	when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
    			"symbIoTe.enablerLogic.syncMessageToEnablerLogic.DefaultEnablerName",
    			(Object) sendMessage,
    			1000)).thenReturn(null);
    	
    	// when
    	ReceivedMessage receivedMessage = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, ReceivedMessage.class, 1000);
    	
    	// then
    	assertThat(receivedMessage).isNull();
    }
    
    @Test
    public void sendingSyncMessageToEnablerLogic_whenExpectingVoid_shouldCallRabbitManagerAndReturnNull() throws Exception {
        // given
        String sendMessage = "test message";
        String enablerName = "enabler name";
        
        when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
                "symbIoTe.enablerLogic.syncMessageToEnablerLogic.DefaultEnablerName",
                (Object) sendMessage)).thenReturn(null);
        
        // when
        Void response = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, Void.class);
        
        // then
        assertThat(response).isNull();
    }
    
    @Test
    public void sendingSyncMessageToEnablerLogicWithTimeout_whenExpectingVoid_shouldCallRabbitManagerAndReturnNull() throws Exception {
    	// given
    	String sendMessage = "test message";
    	String enablerName = "enabler name";
    	
    	when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
    			"symbIoTe.enablerLogic.syncMessageToEnablerLogic.DefaultEnablerName",
    			(Object) sendMessage,
    			1000)).thenReturn(null);
    	
    	// when
    	Void response = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, Void.class, 1000);
    	
    	// then
    	assertThat(response).isNull();
    }
    
    @Test
    public void sendingSyncMessageToEnablerLogic_whenReturnedWrongObjectType_shouldCallRabbitManagerAndThrowException() throws Exception {
        // given
        String sendMessage = "test message";
        String enablerName = "destEnablerName";

        when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
            "symbIoTe.enablerLogic.syncMessageToEnablerLogic.destEnablerName",
            (Object) sendMessage)).thenReturn(Long.valueOf(1));

        assertThatThrownBy(() -> {
            // when
            enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, ReceivedMessage.class);
        })
        
            // then
            .isInstanceOf(WrongResponseException.class)
            .hasFieldOrPropertyWithValue("response", Long.valueOf(1))
            .hasNoCause();
    }

    @Test
    public void sendingSyncMessageToEnablerLogicWithTimeout_whenReturnedWrongObjectType_shouldCallRabbitManagerAndThrowException() throws Exception {
    	// given
    	String sendMessage = "test message";
    	String enablerName = "destEnablerName";
    	
    	when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic",
    			"symbIoTe.enablerLogic.syncMessageToEnablerLogic.destEnablerName",
    			(Object) sendMessage,
    			1000)).thenReturn(Long.valueOf(1));
    	
    	assertThatThrownBy(() -> {
    		// when
    		enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, ReceivedMessage.class, 1000);
    	})
    	
    	// then
    	.isInstanceOf(WrongResponseException.class)
    	.hasFieldOrPropertyWithValue("response", Long.valueOf(1))
    	.hasNoCause();
    }
    
    @Test
    public void registerSyncMessageFromEnablerLogicConsumer_shouldDelegateItToConsumer() throws Exception {
        //given
        Function<String, String> lambda = (m) -> m;

        SyncMessageFromEnablerLogicConsumer syncConsumer = Mockito.mock(SyncMessageFromEnablerLogicConsumer.class);
        enablerLogic.setSyncConsumer(syncConsumer);

        // when
        enablerLogic.registerSyncMessageFromEnablerLogicConsumer(String.class, lambda);

        //then
        verify(syncConsumer).registerReceiver(String.class, lambda);
    }

    @Test
    public void unregisterSyncMessageFromEnablerLogicConsumer_shouldDelegateItInToConsumer() throws Exception {
        //given
        SyncMessageFromEnablerLogicConsumer syncConsumer = Mockito.mock(SyncMessageFromEnablerLogicConsumer.class);
        enablerLogic.setSyncConsumer(syncConsumer);

        // when
        enablerLogic.unregisterSyncMessageFromEnablerLogicConsumer(String.class);

        //then
        verify(syncConsumer).unregisterReceiver(String.class);
    }
}
