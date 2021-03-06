package eu.h2020.symbiote.enablerlogic.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.amqp.core.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoggingTrimHelper {
    public static final int MAX_MESSAGE_BODY_LENGTH = 1000;

    /**
     * This method returns string representation of message. If message body is long it is trimmed.
     * @param msg message
     * @return string representation
     */
    public static String logMsg(Message msg) {
        if(msg == null)
            return null;
        
        StringBuffer buffer = new StringBuffer();
        
        byte bodyArray[] = msg.getBody();
		if(bodyArray.length > MAX_MESSAGE_BODY_LENGTH) {
			byte trimmedBodyArray[] = Arrays.copyOf(bodyArray, MAX_MESSAGE_BODY_LENGTH);
			String body = new String(trimmedBodyArray, StandardCharsets.UTF_8);
			buffer.append(body);
            buffer.append("...' trimmed, originalBodySize=");
            buffer.append(bodyArray.length);
            if (msg.getMessageProperties() != null) {
                buffer.append(" ").append(msg.getMessageProperties().toString());
            }
            buffer.append(")");
            return buffer.toString();
        }
        
        return msg.toString();
    }

    /**
     * This method returns trimmed message if message is longer then 1KB.
     * @param msg message
     * @return string representation
     */
    public static String logToString(String msg) {
        if(msg == null)
            return null;
        
        if(msg.length() > MAX_MESSAGE_BODY_LENGTH)
            return "Trimmed: " + msg.substring(0, MAX_MESSAGE_BODY_LENGTH) + "...";
        return msg;
    }
    
    /**
     * This method returns trimmed object representation if representation is longer then 1KB.
     * @param obj object
     * @return string representation
     */
    public static String logToString(Object obj) {
        if(obj == null)
            return null;
        
        return logToString(obj.toString());
    }
    
    /**
     * This method returns trimmed object JSON representation if representation is longer then 1KB.
     * 
     * If object can not be serialize to JSON then toString method is used.
     * 
     * @param obj object
     * @return JSON representation
     */
    public static String logToJson(Object obj) {
        if(obj == null)
            return null;
        
        ObjectMapper jsonMapper = new ObjectMapper();
        String stringRepresentation;
        try {
            stringRepresentation = jsonMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            stringRepresentation = "Fallback JSON to String: " + obj.toString();
        }
        
        if(stringRepresentation.length() > MAX_MESSAGE_BODY_LENGTH) {
            stringRepresentation = "Trimmed: " + stringRepresentation.substring(0, MAX_MESSAGE_BODY_LENGTH) + "...";
        }

        return stringRepresentation;
    }
}
