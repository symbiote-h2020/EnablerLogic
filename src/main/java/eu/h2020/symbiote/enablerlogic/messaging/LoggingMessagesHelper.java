package eu.h2020.symbiote.enablerlogic.messaging;

import org.springframework.amqp.core.Message;

public class LoggingMessagesHelper {
    private static final int MAX_MESSAGE_BODY_LENGTH = 1000;

    /**
     * This method returns string representation of message. If message body is long it is trimmed.
     * @param msg message
     * @return string representation
     */
    public static String logMsg(Message msg) {
        StringBuffer buffer = new StringBuffer();
        String original = msg.toString();
        
        if(msg.getBody().length > MAX_MESSAGE_BODY_LENGTH) {
            buffer.append(original.substring(0, MAX_MESSAGE_BODY_LENGTH));
            buffer.append("...' trimmed, originalBodySize=");
            buffer.append(msg.getBody().length);
            if (msg.getMessageProperties() != null) {
                buffer.append(" ").append(msg.getMessageProperties().toString());
            }
            buffer.append(")");
            return buffer.toString();
        }
        
        return original;
    }

}
