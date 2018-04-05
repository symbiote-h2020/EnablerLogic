package eu.h2020.symbiote.enablerlogic;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.enablerlogic.messaging.RegistrationHandlerClientService;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.rapplugin.RapPluginConfiguration;
import eu.h2020.symbiote.rapplugin.messaging.RabbitManager;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = EnablerLogicApplicationTest.Application.class)
//@ImportAutoConfiguration({EnablerLogicConfiguration.class, RapPluginConfiguration.class})
//@Import(RabbitManager.class)
@DirtiesContext
public class EnablerLogicApplicationTest {

	@SpringBootApplication
	//@ComponentScan(basePackages={"eu.h2020.symbiote.enablerlogic", "eu.h2020.symbiote.rapplugin", "eu.h2020.symbiote.rapplugin.messaging"})
	public static class Application {
	    @Autowired
	    public EnablerLogicProperties props;
	    
	    @Autowired
	    public RegistrationHandlerClientService rhClientService;
	    
	    @Autowired
	    public RapPlugin rapPlugin;
	
	    @Autowired
	    public EnablerLogic enablerLogic;

	    public static void main(String[] args) throws Exception {
			SpringApplication.run(EnablerLogicApplicationTest.Application.class, args);
		}
	    
//	    @Bean
//	    public RabbitManager rapRabbitManagerForTesting(RabbitTemplate template) {
//	    	return new RabbitManager(template);
//	    }
	}
	
    @Test
	public void testConfigurationStarted() throws Exception {
		
	}
}
