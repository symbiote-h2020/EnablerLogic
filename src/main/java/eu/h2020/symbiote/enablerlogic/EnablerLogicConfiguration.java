package eu.h2020.symbiote.enablerlogic;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import eu.h2020.symbiote.client.RegistrationHandlerClient;
import eu.h2020.symbiote.enablerlogic.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.PluginProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.enablerlogic.messaging.properties.RoutingKeysProperties;
import eu.h2020.symbiote.rapplugin.RapPluginConfiguration;


@Configuration
@ComponentScan(basePackages = {"eu.h2020.symbiote.enablerlogic"})
@EnableConfigurationProperties({
    RabbitConnectionProperties.class, 
    RoutingKeysProperties.class, 
    ExchangeProperties.class, 
    PluginProperties.class})
@EnableDiscoveryClient
@EnableFeignClients
@AutoConfigureAfter(RapPluginConfiguration.class)
public class EnablerLogicConfiguration implements ApplicationContextAware, SmartLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(EnablerLogicConfiguration.class);
    
    @Autowired
    private ConfigurableApplicationContext ctx;
    
    @Autowired
    private Collection<ProcessingLogic> processingLogic;
    
    @Autowired
    private EnablerLogic enablerLogic;
    
    @Autowired
    private DiscoveryClient discoveryClient;
        
    private volatile boolean running = false;
    
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
        new Thread(() -> {
            // wait for discovery client
            discoveryClient.getServices();
           // TODO remove
//            List<ServiceInstance> si = discoveryClient.getInstances("RegistrationHandler");
//            int counter = 0;
//            while(si.isEmpty()) {
//                if((counter % 10) == 0)
//                   LOG.debug("Waiting for RegistrationHandler in Eureka. counter={}", counter);
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                }
//                si = discoveryClient.getInstances("RegistrationHandler");
//                if(counter >= 3000) {
//                    LOG.error("Waiting maximal time of 5min for RegistartionHandler in Eureka");
//                    return;
//                }
//            }
//            
//            LOG.debug("RegistrationHandler at {}", si.get(0).getUri());
            
            // Wait for other things to start up.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }            
            
            processingLogic.forEach((pl) -> {
            	try {
            		pl.initialization(enablerLogic);
            	} catch (Throwable t) {
            		LOG.error("Turning down enabler because of error in initialization", t);
            		
            		SpringApplication.exit(ctx, () -> {
            			// here put some other things that need to be closed befor shutdown
            			return 1;
            		});
            		// or System.exit(1)
            	}
            });
            running = true;
        }).start();
    }

    @Override
    public void stop() {
        LOG.debug("STOP");
        running = false;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
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
