package eu.h2020.symbiote.enablerlogic.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "rabbit", ignoreInvalidFields = true)
public class RabbitConnectionProperties {
    private String host = "localhost";
    private String username = "guest";
    private String password = "guest";
    private int replyTimeout = 5000;
}


