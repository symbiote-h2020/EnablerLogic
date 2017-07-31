package eu.h2020.symbiote;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.messaging.RabbitManager;
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
}
