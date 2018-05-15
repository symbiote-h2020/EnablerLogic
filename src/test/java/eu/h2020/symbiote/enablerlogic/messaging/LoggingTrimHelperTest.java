package eu.h2020.symbiote.enablerlogic.messaging;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

public class LoggingTrimHelperTest {

	@Test
	public void logMsg_withNullMessage_shouldReturnNull() {
		assertThat(LoggingTrimHelper.logMsg(null)).isNull();
	}

	@Test
	public void logMsg_withSmallMessage_shouldReturnThatMessage() {
		MessageProperties msgProps = new MessageProperties();
		msgProps.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
		Message msg = new Message("msg".getBytes(), msgProps);

		assertThat(LoggingTrimHelper.logMsg(msg)).startsWith("(Body:'msg' MessageProperties");
	}

	@Test
	public void logMsg_withMessageBiggerThen1K_shouldReturnTrimmedMessage() {
		MessageProperties msgProps = new MessageProperties();
		msgProps.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
		
		Message msg = new Message(bigString().getBytes(), msgProps);

		assertThat(LoggingTrimHelper.logMsg(msg).substring(LoggingTrimHelper.MAX_MESSAGE_BODY_LENGTH))
			.startsWith("...' trimmed, originalBodySize=2000 MessageProperties");
	}

	@Test
	public void logMsg_withMessageBiggerThen1KAndNullMsgProperties_shouldReturnTrimmedMessage() {
		Message msg = new Message(bigString().getBytes(), null);
		
		assertThat(LoggingTrimHelper.logMsg(msg).substring(LoggingTrimHelper.MAX_MESSAGE_BODY_LENGTH))
			.isEqualTo("...' trimmed, originalBodySize=2000)");
	}
	
	@Test
	public void logToString_withNullString_shouldReturnNull() {
		assertThat(LoggingTrimHelper.logToString(null)).isNull();
	}

	@Test
	public void logToString_withSmallString_shouldReturnThatString() {
		assertThat(LoggingTrimHelper.logToString("msg"))
			.isEqualTo("msg");
	}

	@Test
	public void logToString_withStringBiggerThen1K_shouldReturnThatString() {
		assertThat(LoggingTrimHelper.logToString(bigString()))
			.startsWith("Trimmed: ")
			.endsWith("...")
			.hasSize(LoggingTrimHelper.MAX_MESSAGE_BODY_LENGTH+12);
	}

	@Test
	public void logToString_withNullObject_shouldReturnNull() {
		Object o = null;
		assertThat(LoggingTrimHelper.logToString(o)).isNull();
	}

	@Test
	public void logToString_withSmallObject_shouldReturnThatString() {
		Object msg = "msg";
		assertThat(LoggingTrimHelper.logToString(msg))
			.isEqualTo(msg);
	}

	@Test
	public void logToJson_withNullObject_shouldReturnNull() {
		Object o = null;
		assertThat(LoggingTrimHelper.logToJson(o)).isNull();
	}

	@Test
	public void logToJson_withSmallObject_shouldReturnJsonRepresentation() {
		Object o = "msg";
		assertThat(LoggingTrimHelper.logToJson(o)).isEqualTo("\"msg\"");
	}

	@Test
	public void logToJson_withObjectBiggerThen1K_shouldReturnJsonRepresentation() {
		Object o = bigString();
		assertThat(LoggingTrimHelper.logToJson(o))
			.startsWith("Trimmed: \"")
			.endsWith("...")
			.hasSize(LoggingTrimHelper.MAX_MESSAGE_BODY_LENGTH+12);
	}
	
	private String bigString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i < 2000; i++)
			sb.append('m');
		String msg = sb.toString();
		return msg;
	}	
}
