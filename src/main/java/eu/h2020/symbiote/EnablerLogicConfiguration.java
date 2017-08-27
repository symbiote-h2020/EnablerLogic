package eu.h2020.symbiote;

import eu.h2020.symbiote.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan
@EnableConfigurationProperties({RabbitConnectionProperties.class, RoutingKeysProperties.class, ExchangeProperties.class})
public class EnablerLogicConfiguration implements ApplicationContextAware, SmartLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(EnablerLogicConfiguration.class);
    
    @Autowired
    private Collection<ProcessingLogic> processingLogic;
    
    @Autowired
    private EnablerLogic enablerLogic;
        
    private boolean running = false;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        // print bean names in context
        LOG.debug("ALL BEANS: " + Arrays.toString(ctx.getBeanDefinitionNames()));
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void start() {
        LOG.debug("START");
        processingLogic.forEach((pl) -> pl.initialization(enablerLogic));
        running = true;
    }

    @Override
    public void stop() {
        LOG.debug("STOP");
        running = false;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        LOG.debug("STOP Runnable");
        
        running = false;
        callback.run();
    }
}
