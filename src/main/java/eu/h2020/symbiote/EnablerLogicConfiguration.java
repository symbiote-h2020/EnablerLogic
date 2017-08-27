package eu.h2020.symbiote;

import eu.h2020.symbiote.messaging.consumers.RabbitListenerConsumer;
import eu.h2020.symbiote.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;


@Configuration
@ComponentScan
@EnableConfigurationProperties({RabbitConnectionProperties.class, RoutingKeysProperties.class, ExchangeProperties.class})
public class EnablerLogicConfiguration implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(EnablerLogicConfiguration.class);
    
    @Autowired
    private Collection<ProcessingLogic> processingLogic;
    
    @Autowired
    private Collection<RabbitListenerConsumer> rabbitConsumers;
    
    @Autowired
    private EnablerLogic enablerLogic;
    
    @Autowired
    private RabbitListenerEndpointRegistry rabbitListenerRegistry;

    @PostConstruct
    public void init() {
        new Thread(() -> asyncInit()).start();
    }
    
    private void asyncInit() {
        waitForRabbitListeneresToStart(countRabbitListeners());
        
        processingLogic.forEach((pl) -> pl.initialization(enablerLogic));
    }

    private int countRabbitListeners() {
        int count = 0;
        for(Object o: rabbitConsumers) {
            Method[] methods = o.getClass().getMethods();
            for(Method m: methods) {
                if(AnnotatedElementUtils.hasAnnotation(m, RabbitListener.class))
                    count++;
            }
        }
        return count;
    }

    private void waitForRabbitListeneresToStart(int count) {
        // check that expected number of containers are
        while(rabbitListenerRegistry.getListenerContainers().size() != count) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        
        boolean allRunning = true;
        do {
            allRunning = true;
            for(MessageListenerContainer container: rabbitListenerRegistry.getListenerContainers()) {
                allRunning &= container.isRunning();
            }
            
            if(!allRunning) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        } while(!allRunning);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        // print bean names in context
        LOG.debug("ALL BEANS: " + Arrays.toString(ctx.getBeanDefinitionNames()));
        
        // TODO isbrisati
        Object o1 = ctx.getBean("org.springframework.amqp.rabbit.config.internalRabbitListenerAnnotationProcessor");
        LOG.debug("org.springframework.amqp.rabbit.config.internalRabbitListenerAnnotationProcessor class:{}", o1.getClass().getName());
        Object o2 = ctx.getBean("org.springframework.amqp.rabbit.config.internalRabbitListenerEndpointRegistry");
        LOG.debug("org.springframework.amqp.rabbit.config.internalRabbitListenerEndpointRegistry class:{}", o2.getClass().getName());
        Object o3 = ctx.getBean("rabbitListenerContainerFactoryConfigurer");
        LOG.debug("rabbitListenerContainerFactoryConfigurer class:{}", o3.getClass().getName());
        Object o4 = ctx.getBean("rabbitListenerContainerFactory");
        LOG.debug("rabbitListenerContainerFactory class:{}", o4.getClass().getName());
    }
}
