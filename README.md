[![Build Status](https://api.travis-ci.org/symbiote-h2020/EnablerLogic.svg?branch=develop)](https://api.travis-ci.org/symbiote-h2020/EnablerLogic)
[![codecov.io](https://codecov.io/github/symbiote-h2020/EnablerLogic/branch/staging/graph/badge.svg)](https://codecov.io/github/symbiote-h2020/EnablerLogic)
[![](https://jitpack.io/v/symbiote-h2020/EnablerLogic.svg)](https://jitpack.io/#symbiote-h2020/EnablerLogic)

# EnablerLogic

## Using Generic EnablerLogic

The idea of Generic EnablerLogic is to use it as dependency in specific EnablerLogic. 
Generic parts like RabbitMQ communication with other components in enabler (e.g. ResourceManager, 
PlatformProxy, ...) are implemented in Generic EnablerLogic. That way a developer of specific enabler 
doesn't have to implement complex communication between those components. 

## Creating specific EnablerLogic

### 1. Creating new SpringBoot project

	- It needs following dependencies: Config Client, Eureka Discovery, Zipkin Client

### 2. Adding symbIoTe dependencies to `build.gradle`

	- Add following dependencies:

		``compile('com.github.symbiote-h2020:EnablerLogic:develop-SNAPSHOT') { changing = true }``

		- This is dependency to development version of EnablerLogic from jitpack. It will use the newest version of 
		EnablerLogic published in jitpack. This is only for development. In the future this will be 
		published in some official repository. In order to use jitpack you need to put in `build.gradle` 
		following lines as well:

			```
			allprojects {
				repositories {
					jcenter()
					maven { url "https://jitpack.io" }
				}
			}
			```

### 3. Setting configuration

	- Configuration needs to be put in `bootstrap.properties` or YMl file. An example is here:

	```
	spring.application.name=EnablerLogicExample
	spring.cloud.config.uri=http://localhost:8888
	```

	- The first line is defining the name of this specific EnablerLogic. Under this name properties 
	are loaded from config server.

	- The second line is location of config server. This is the case when config server is run in 
	local machine which is suitable for development.

### 4. Creating ProcessingLogic component

	- Each enabler must have one ProcessingLogic component. This component implements 
	`eu.h2020.symbiote.enablerlogic.ProcessingLogic` interface.

	- There are methods that are called upon receiving messages (over RabbitMQ) from 
	other enabler components.

	- The most important method is `init` that is called when the enabler is started. It 
	has as parameter `EnablerLogic` object that has methods for sending messages to other 
	components.

	- Here is an example of one component:

	```java
	@Component
	public class InterpolatorLogic implements ProcessingLogic {
		private static final Logger log = LoggerFactory.getLogger(InterpolatorLogic.class);

		private EnablerLogic enablerLogic;

		@Override
		public void initialization(EnablerLogic enablerLogic) {
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
### 5. Communication with other Enabler Logic components in the enabler

#### Asynchronous Communication

##### Asynchronous Receiver

In the initialization of `ProcessingLogic` you need to register consumer that will 
receive messages form other Enabler Logic components. Consumer needs to implement
functional interface `java.util.function.Consumer<T>`. For registering consumer
there is method in `EnablerLogic` class 
`registerAsyncMessageFromEnablerLogicConsumer`. This method as arguments accept:

* `Class<O> clazz` - Class of object that can be consumed.
* `Consumer<O> consumer` - Object that is called when specified type of message is received

There is also method for unregistering consumer
`unregisterAsyncMessageFromEnablerLogicConsumer`. Only one consumer can be 
registered for one type (class).

If there is no type registered for the message that is received 
`WrongRequestException` will be logged as ERROR in the console.  

Example:
```
enablerLogic.registerAsyncMessageFromEnablerLogicConsumer(
    MessageRequest.class, 
    (m) -> log.info("Received from another EnablerLogic: {}", m));
``` 
 
##### Asynchronous Sending 

There is `sendAsyncMessageToEnablerLogic` method in `EnablerLogic` class that 
is used for sending message to another Enabler Logic component. The parameters are:

* `String enablerName` - the name of another Enabler Logic. When you are 
building enabler you will know which other Enabler Logic components are part 
of that enabler so there is no need to have flexible discovery of other Enabler 
Logic components.

* `Object msg` - the object that will be serialized into JSON and send to other
Enabler Logic component.

Example:
```
enablerLogic.sendAsyncMessageToEnablerLogic(
    "EnablerLogicInterpolator", 
    new MessageRequest());
```

#### Synchronous Communication

##### Synchronous Receiver

In the initialization of `ProcessingLogic` you need to register consumer that will 
receive messages form other Enabler Logic components handle it and return response. 
Consumer needs to implement functional interface `java.util.function.Function<O,T>`.
`<O>` is request type and `<T>` is response type.  
For registering consumer there is method in `EnablerLogic` class 
`registerSyncMessageFromEnablerLogicConsumer`. This method accepts following arguments:

* `Class<O> clazz` - Class of object that can be consumed (request).
* `Function<O, ?> function` - Object that is called when specified type of message is received

There is also method for unregistering consumer
`unregisterSyncMessageFromEnablerLogicConsumer`. Only one consumer can be 
registered for one type (class).

If there is no type registered for the message that is received 
`WrongRequestException` will be returns instead of real object.


Example:
```
enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
    MessageRequest.class, 
    (m) -> new MessageResponse("response: " + m.getRequest()));
``` 

##### Synchronous Sender
 
There is `sendSyncMessageToEnablerLogic` method in `EnablerLogic` class that 
is used for sending message to another Enabler Logic component. The parameters are:

* `String enablerName` - the name of another Enabler Logic. When you are 
building enabler you will know which other Enabler Logic components are part 
of that enabler so there is no need to have flexible discovery of other Enabler 
Logic components.

* `Object msg` - the object that will be serialized into JSON and send to other
Enabler Logic component.

* `Class<O> clazz` - the class of expected response object 

If the type of the response message can not be casted to clazz then 
`WrongResponseException` is thrown. The exception will contain the object that is returned.

In the case of timeout `null` will be returned.

Example:
```
MessageResponse response = enablerLogic.sendSyncMessageToEnablerLogic(
    "EnablerLogicInterpolator",
    new MessageRequest("request"),
    MessageResponse.class);
```

## Running

You can run this enabler as any other spring boot application.

``./gradlew bootRun``

or

``java -jar build/libs/EnablerLogicExample-0.0.1-SNAPSHOT.jar``

Note: In order to function correctly you need to start following components before: 
RabbitMQ server, Config Server, Eureka and Zipkin.
