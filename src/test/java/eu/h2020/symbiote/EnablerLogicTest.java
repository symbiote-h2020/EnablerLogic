package eu.h2020.symbiote;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.messaging.consumers.AsyncMessageFromEnablerLogicConsumer;
import eu.h2020.symbiote.messaging.properties.EnablerLogicProperties;

@RunWith(MockitoJUnitRunner.class)
public class EnablerLogicTest {
    private EnablerLogic enablerLogic;
    
    @Mock
    RabbitManager rabbitManager;

    @Before
    public void setup() {
        enablerLogic = new EnablerLogic(rabbitManager, new EnablerLogicProperties());
    }
    
    @Test
    public void queryResourceManagerWithOneRequest_shouldReturnResponse() throws Exception {
        // given
        ArgumentCaptor<ResourceManagerAcquisitionStartRequest> captor = ArgumentCaptor.forClass(ResourceManagerAcquisitionStartRequest.class);
        ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest();
        ResourceManagerAcquisitionStartResponse response = new ResourceManagerAcquisitionStartResponse();
        when(rabbitManager.sendRpcMessage(
                eq("symbIoTe.resourceManager"), 
                eq("symbIoTe.resourceManager.startDataAcquisition"), 
                any(ResourceManagerAcquisitionStartRequest.class))).thenReturn(response);
        
        // when
        enablerLogic.queryResourceManager(request);
        
        // then
        verify(rabbitManager).sendRpcMessage(eq("symbIoTe.resourceManager"), 
                eq("symbIoTe.resourceManager.startDataAcquisition"), 
                captor.capture());
        ResourceManagerAcquisitionStartRequest requests = captor.getValue();
        assertThat(requests.getResources()).hasSize(1);
        assertThat(requests.getResources()).contains(request);
    }
    
    @Test
    public void sendingSyncMessageToEnablerLogic_shouldCallRabbitManager() throws Exception {
        // given
        //ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        String message = "test message";
        String enablerName = "enabler name";
        
        // when
        enablerLogic.sendAsyncMessageToEnablerLogic(enablerName, message);
        
        // then
        verify(rabbitManager).sendMessage(eq("symbIoTe.enablerLogic"), 
                eq("symbIoTe.enablerLogic.asyncMessageToEnablerLogic.DefaultEnablerName"), 
                eq((Object)message));
    }
    
    @Test
    public void registerAsyncMessageFromEnablerLogicConsumer_shouldDelegateItToConsumer() throws Exception {
        //given
        Consumer<String> lambda = (m) -> {};

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
}
