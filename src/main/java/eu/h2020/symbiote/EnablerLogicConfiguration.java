package eu.h2020.symbiote;

import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.repository.EnablerLogicDataAppearedMessageRepository;
import eu.h2020.symbiote.repository.ResourceManagerTaskInfoResponseRepository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@Configuration
@ComponentScan
// TODO configurationProperties
public class EnablerLogicConfiguration {
	private static Log log = LogFactory.getLog(EnablerLogicConfiguration.class);
	
    @Autowired
    private ProcessingLogic processingLogic;

    @Autowired
    private EnablerLogic enablerLogic;

//	@Autowired
//	ResourceManagerTaskInfoResponseRepository tasksRepo;
//	
//	@Autowired
//	EnablerLogicDataAppearedMessageRepository dataRepo;


    @PostConstruct
    public void init() {
    		processingLogic.init(enablerLogic);
    }

//    @Component
//    public static class CLR implements CommandLineRunner {
//
//        private final RabbitManager rabbitManager;
//
//        @Autowired
//        public CLR(RabbitManager rabbitManager) {
//            this.rabbitManager = rabbitManager;
//        }
//
//        @Override
//        public void run(String... args) throws Exception {
//
//            //message retrieval - start rabbit exchange and consumers
//            this.rabbitManager.init();
//            log.info("CLR run() and Rabbit Manager init()");
//        }
//    }
    
    @PreDestroy
    public void cleanUp() throws Exception {
  	  log.info("EnablerLogic shutting down...");  
  	  log.info("Removing all entities from MongoDB...");
//  	  tasksRepo.deleteAll();
//  	  dataRepo.deleteAll();
  	}
}