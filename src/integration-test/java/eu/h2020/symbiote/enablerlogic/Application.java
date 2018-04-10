package eu.h2020.symbiote.enablerlogic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import eu.h2020.symbiote.enablerlogic.messaging.RegistrationHandlerClientService;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;

@SpringBootApplication
public class Application {
    @Autowired
    public EnablerLogicProperties props;
    
    @Autowired
    public RegistrationHandlerClientService rhClientService;
    
    @Autowired
    public RapPlugin rapPlugin;

    @Autowired
    public EnablerLogic enablerLogic;

    public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}
}