package eu.h2020.symbiote.messaging;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@RunWith(MockitoJUnitRunner.class)
public class RabbitManagerTests {
	
	@Mock
	RabbitTemplate rabbitTemplate;
	
	RabbitManager rabbitManager;
	
	
	@Before
	public void setup() {
		rabbitManager = new RabbitManager(rabbitTemplate);
	}

	@Test
	public void sendMessageWithMessage_shouldCallRabbitTemplate() throws Exception {
		// given
		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		String exchange = "e";
		String key = "k";
		
		// when
		rabbitManager.sendMessage(exchange, key, "m");
		
		// then
		verify(rabbitTemplate).send(eq(exchange), eq(key), messageCaptor.capture());
		Message message = messageCaptor.getValue();
		assertThat(message.getBody()).isEqualTo("m".getBytes(StandardCharsets.UTF_8));
		assertThat(message.getMessageProperties().getContentType()).isEqualTo("plain/text");
	}

	@Test
	public void sendMessageWithObject_shouldCallRabbitTemplate() throws Exception {
		// given
		String exchange = "e";
		String key = "k";
		Object obj = new Object();
		
		// when
		rabbitManager.sendMessage(exchange, key, obj);
		
		// then
		verify(rabbitTemplate).convertAndSend(exchange, key, obj);
	}
	
	
}
