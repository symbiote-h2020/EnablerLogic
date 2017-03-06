package eu.h2020.symbiote;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.model.Location;
import eu.h2020.symbiote.model.Resource;
import eu.h2020.symbiote.model.RpcResourceResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * Cloud-Core Interface module's entry point.
 * <p>
 * Cloud-Core Interface is a southbound interface for accessing symbIoTe platform.
 * It allows platform owners to register their resources.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class EnablerLogicApplication {

	private static Log log = LogFactory.getLog(EnablerLogicApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(EnablerLogicApplication.class, args);
    }

    @Component
    public static class CLR implements CommandLineRunner {

        private final RabbitManager rabbitManager;

        @Autowired
        public CLR( RabbitManager rabbitManager) {
            this.rabbitManager = rabbitManager;
        }

        @Override
        public void run(String... args) throws Exception {
            this.rabbitManager.initCommunication();
        }
    }

    @Bean
    public AlwaysSampler defaultSampler() {
        return new AlwaysSampler();
    }

}
