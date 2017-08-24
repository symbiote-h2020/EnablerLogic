package eu.h2020.symbiote.messaging.consumers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.bin.RabbitMqPlugins;

public class EmbeddedRabbitFixture {
    private static final int RABBIT_STARTING_TIMEOUT = 10_000;

    protected static EmbeddedRabbitMq rabbitMq;
    
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @BeforeClass
    public static void startEmbeddedRabbit() throws Exception {
        EmbeddedRabbitMqConfig config = new EmbeddedRabbitMqConfig.Builder()
            .rabbitMqServerInitializationTimeoutInMillis(RABBIT_STARTING_TIMEOUT)
            .build();

        cleanupVarDir(config);
            
        rabbitMq = new EmbeddedRabbitMq(config);
        rabbitMq.start();
    
        RabbitMqPlugins rabbitMqPlugins = new RabbitMqPlugins(config);
        rabbitMqPlugins.enable("rabbitmq_management");
        rabbitMqPlugins.enable("rabbitmq_tracing");
    }

    private static void cleanupVarDir(EmbeddedRabbitMqConfig config) throws IOException {
        File varDir = new File(config.getAppFolder(), "var");
        if(varDir.exists())
            FileUtils.cleanDirectory(varDir);
    }

    @AfterClass
    public static void stopEmbeddedRabbit() {
        rabbitMq.stop();
    }
}
