package eu.h2020.symbiote.enablerlogic;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, EnablerLogicProperties.class},
		properties="symbIoTe.interworking.interface.url=http://localhost:8080")
@TestPropertySource(locations = "classpath:integration.properties")
@DirtiesContext
public class EnablerLogicApplicationTest {

	@Test
	public void testConfigurationStarted() throws Exception {
	}
}
