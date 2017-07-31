package eu.h2020.symbiote;

import eu.h2020.symbiote.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan
@EnableConfigurationProperties({RabbitConnectionProperties.class, RoutingKeysProperties.class, ExchangeProperties.class})
public class EnablerLogicConfiguration {
    @Autowired
    private Collection<ProcessingLogic> processingLogic;

    @Autowired
    private EnablerLogic enablerLogic;

    @PostConstruct
    public void init() {
    		processingLogic.forEach((pl) -> pl.init(enablerLogic));
    }
}