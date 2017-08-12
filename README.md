[![Build Status](https://api.travis-ci.org/symbiote-h2020/EnablerLogic.svg?branch=staging)](https://api.travis-ci.org/symbiote-h2020/EnablerLogic)
[![codecov.io](https://codecov.io/github/symbiote-h2020/EnablerLogic/branch/staging/graph/badge.svg)](https://codecov.io/github/symbiote-h2020/EnablerLogic)

# EnablerLogic

## Using Generic EnablerLogic

In order to create specific EnablerLogic you need first to download source of this generic EnablerLogic, compile it
and install in local maven repository.

1. Downloading generic Enabler Logic

	``git clone https://github.com/symbiote-h2020/EnablerLogic.git``

2. Compile and install in local maven repository

	``./gradlew build publishToMavenLocal``

## Creating specific EnablerLogic

1. Creating new SpringBoot project

	- It needs following dependencies: Config Client, Eureka Discovery, Zipkin Client

2. Adding symbIoTe dependencies to `build.gradle`

	- Add following dependencies:

		``compile('com.github.symbiote-h2020:SymbIoTeLibraries:develop-SNAPSHOT') { changing = true }``
		``compile('eu.h2020.symbiote:EnablerLogic:0.0.1-SNAPSHOT')``

		- The first is dependency to SymbIoTeLibraries from jitpack. It will use current verision of SymbIoTeLibraries published in jitpack. This is only for development. In the future this will be published in some official repository. In order to use jitpack you need to put in `build.gradle` following lines as well:

			```
			allprojects {
				repositories {
					jcenter()
					maven { url "https://jitpack.io" }
				}
			}
			```

		- The second dependency is for generic part of EnablerLogic that was just installed in local maven repository. Be careful that the version is the same as the version in cloned repository.

			- You can check version in cloned repository by looking in `build.gradle` for the following line:

				``version = '0.0.1-SNAPSHOT'``

3. Setting configuration

	- Configuration needs to be put in `bootstrap.properties` or YMl file. An example is here:

	```
	spring.application.name=EnablerLogicExample
	spring.cloud.config.uri=http://localhost:8888
	```

	- The first line is defining the name of this specific EnablerLogic. Under this name properties are loaded from config server.

	- The second line is location of config server. This is the case when config server is run in local machine which is suitable for development.

4. Creating ProcessingLogic component

	- Each enabler must have one ProcessingLogic component. This component implements `eu.h2020.symbiote.ProcessingLogic` interface.

	- There are methods that are called upon receiving messages (over RabbitMQ) from other enabler components.

	- The most important method is `init` that is called when the enabler is started. It has as parameter `EnablerLogic` object that has methods for sending messages to other components.

	- Here is an example of one component:

	```java
	@Component
	public class InterpolatorLogic implements ProcessingLogic {
		private static final Logger log = LoggerFactory.getLogger(InterpolatorLogic.class);

		private EnablerLogic enablerLogic;

		@Override
		public void init(EnablerLogic enablerLogic) {
			this.enablerLogic = enablerLogic;

			sendQuery();
		}

		@Override
		public void measurementReceived(EnablerLogicDataAppearedMessage dataAppeared) {
			System.out.println("received new Observations:\n"+dataAppeared);
		}

		private void sendQuery() {
			ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest();
			request.setTaskId("someId");
			request.setEnablerLogicName("exampleEnabler");
			request.setMinNoResources(1);
			request.setCachingInterval_ms(3600L);

			CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
			coreQueryRequest.setLocation_lat(48.208174);
			coreQueryRequest.setLocation_long(16.373819);
			coreQueryRequest.setMax_distance(10_000); // radius 10km
			coreQueryRequest.setObserved_property(Arrays.asList("NOx"));
			request.setCoreQueryRequest(coreQueryRequest);
			ResourceManagerAcquisitionStartResponse response = enablerLogic.queryResourceManager(request);

			try {
				log.info("querying fixed resources: {}", new ObjectMapper().writeValueAsString(response));
			} catch (JsonProcessingException e) {
				log.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
			}
		}
	}
	```

## Running

You can run this enabler as any other spring boot application.

``./gradlew bootRun``

or

``java -jar build/libs/EnablerLogicExample-0.0.1-SNAPSHOT.jar``

Note: In order to function correctly you need to start following components before: RabbitMQ server, Config Server, Eureka and Zipkin.
