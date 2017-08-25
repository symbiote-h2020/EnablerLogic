package eu.h2020.symbiote;

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

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.messaging.WrongResponseException;
import eu.h2020.symbiote.messaging.consumers.AsyncMessageFromEnablerLogicConsumer;
import eu.h2020.symbiote.messaging.consumers.SyncMessageFromEnablerLogicConsumer;
import eu.h2020.symbiote.messaging.properties.EnablerLogicProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
    
    @Test
    public void sendingAsyncMessageToEnablerLogic_shouldCallRabbitManager() throws Exception {
        // given
        String message = "test message";
        String enablerName = "enabler name";
        
        // when
        enablerLogic.sendAsyncMessageToEnablerLogic(enablerName, message);
        
        // then
        verify(rabbitManager).sendMessage(eq("symbIoTe.enablerLogic"), 
                eq("symbIoTe.enablerLogic.asyncMessageToEnablerLogic.DefaultEnablerName"), 
                eq((Object)message));
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
        String enablerName = "enabler name";
        ReceivedMessage mockReceiveMessage = new ReceivedMessage("received message");
        
        when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic", 
                "symbIoTe.enablerLogic.syncMessageToEnablerLogic.DefaultEnablerName", 
                (Object)sendMessage)).thenReturn(mockReceiveMessage);
        
        // when
        ReceivedMessage receivedMessage = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, ReceivedMessage.class);
        
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
                (Object)sendMessage)).thenReturn(null);
        
        // when
        ReceivedMessage receivedMessage = enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, ReceivedMessage.class);
        
        // then
        assertThat(receivedMessage).isNull();
    }

    @Test
    public void sendingSyncMessageToEnablerLogic_whenReturnedWrongObjectType_shouldCallRabbitManagerAndThrowException() throws Exception {
        // given
        String sendMessage = "test message";
        String enablerName = "enabler name";
        
        when(rabbitManager.sendRpcMessage("symbIoTe.enablerLogic", 
                "symbIoTe.enablerLogic.syncMessageToEnablerLogic.DefaultEnablerName", 
                (Object)sendMessage)).thenReturn(new Long(1));
        
        assertThatThrownBy(() -> {
            // when
            enablerLogic.sendSyncMessageToEnablerLogic(enablerName, sendMessage, ReceivedMessage.class);
        })
        // then
        .isInstanceOf(WrongResponseException.class)
        .hasFieldOrPropertyWithValue("response", new Long(1))
        .hasNoCause();
    }
    
    @Test
    public void registerSyncMessageFromEnablerLogicConsumer_shouldDelegateItToConsumer() throws Exception {
        //given
        Function<String,String> lambda = (m) -> m;
        
        SyncMessageFromEnablerLogicConsumer syncConsumer = Mockito.mock(SyncMessageFromEnablerLogicConsumer.class);
        enablerLogic.setSyncConsumer(syncConsumer);
        
        // when
        enablerLogic.registerSyncMessageFromEnablerLogicConsumer(String.class, lambda);
        
        //then
        verify(syncConsumer).registerReceiver(String.class, lambda);
    }
    
    @Test
    public void unregisterSyncMessageFromEnablerLogicConsumer_shouldDelegateItToConsumer() throws Exception {
        //given
        SyncMessageFromEnablerLogicConsumer syncConsumer = Mockito.mock(SyncMessageFromEnablerLogicConsumer.class);
        enablerLogic.setSyncConsumer(syncConsumer);
        
        // when
        enablerLogic.unregisterSyncMessageFromEnablerLogicConsumer(String.class);
        
        //then
        verify(syncConsumer).unregisterReceiver(String.class);
    }
    

}
