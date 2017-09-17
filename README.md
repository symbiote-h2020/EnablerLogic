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

It needs following dependencies: Config Client, Eureka Discovery, Zipkin Client

### 2. Adding symbIoTe dependencies to `build.gradle`

Add following dependencies:

`compile('com.github.symbiote-h2020:EnablerLogic:develop-SNAPSHOT') { changing = true }`

This is dependency to development version of EnablerLogic from jitpack. It will use the newest version of 
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

Configuration needs to be put in `bootstrap.properties` or YMl file. An example is here:

```
spring.application.name=EnablerLogicExample
spring.cloud.config.uri=http://localhost:8888
```

The first line is defining the name of this specific EnablerLogic. Under this name properties 
are loaded from config server.

The second line is location of config server. This is the case when config server is run in 
local machine which is suitable for development.

### 4. Creating ProcessingLogic component

Each enabler must have one ProcessingLogic component. This component implements 
`eu.h2020.symbiote.enablerlogic.ProcessingLogic` interface or extend 
`eu.h2020.symbiote.enablerlogic.ProcessingAdapter` and override some methods.

The most important method is `initialization` that is called when the enabler is started. It 
has as parameter `EnablerLogic` object that has methods for sending messages to other 
components.

Here is an example of one component:

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
	...
}
```

There are also methods that are called upon receiving messages (over RabbitMQ) from 
other enabler components. Here is list of them:

- `void measurementReceived(EnablerLogicDataAppearedMessage dataAppearedMessage)` - 
this method is called when sensor data from Platform Proxy component is received.

- `void notEnoughResources(NotEnoughResourcesAvailable notEnoughResourcesAvailableMessage)` -
this method is called when Resource Manager component can not find enough 
resources for specified acquisition taskId.

- `void resourcesUpdated(ResourcesUpdated resourcesUpdatedMessage)` -
this method is called when Resource Manager component has updated resources 
for specified acquisition taskId.

#### Enabler Logic communicating with Resource Manager or Platform Proxy components

- `public ResourceManagerAcquisitionStartResponse queryResourceManager(ResourceManagerTaskInfoRequest...requests)` - 
this method sends to Registration Manager to start data acquisition. The argument
is request for resources and the result contains list of resources and taskId.

- `public CancelTaskResponse cancelTask(CancelTaskRequest request)` -
this method sends to Resource Manager to cancel acquisition task request.

- `public ResourceManagerUpdateResponse updateTask(ResourceManagerUpdateRequest request)` -
this method sends to Resource Manager to request for update acquisition task.

- `public void reportBrokenResource(ProblematicResourcesMessage message)` -
this method sends message to Resource Manager that specified broken resource 
is producing wrong data.

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
```java
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
```java
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
```java
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
```java
MessageResponse response = enablerLogic.sendSyncMessageToEnablerLogic(
    "EnablerLogicInterpolator",
    new MessageRequest("request"),
    MessageResponse.class);
```

### 6. Registering resources

Registration of resources is going by communicating with Registration Handler
component. This communication is REST based. For getting URL Eureka discovery
server is used. In the enabler spring boot application discovery client 
need to be enabled by putting `@EnableDiscoveryClient` in configuration:

```java
@SpringBootApplication
@EnableDiscoveryClient
public class EnablerLogicInterpolator {

    public static void main(String[] args) {
        SpringApplication.run(EnablerLogicInterpolator.class, args);
    }
}
``` 

For communication with Registration Handler component there is service
`RegistrationHandlerClientService` which can be injected in any spring
component like this:

```java
@Autowired
private RegistrationHandlerClientService rhClientService;
```

There are different methods for registering, unregistering and updating
resources. Each of this methods return list of `CloudResource` objects 
that are changed by this method.

In the `CloudResoure` class should be put plugin id. The plugin id
can be obtained from  `EnablerLogicProperties` object that can be
injected. The method `getEnablerName()` returns plugin id.

Here is example of registration:
```java
public class InterpolatorLogic implements ProcessingLogic {
...
    
    @Autowired
    private EnablerLogicProperties props;
    
    @Autowired
    private RegistrationHandlerClientService rhClientService;

    @Override
    public void initialization(EnablerLogic enablerLogic) {
        this.enablerLogic = enablerLogic;

        registerResources();
        ...
    }

    private void registerResources() {
        List<CloudResource> cloudResources = new LinkedList<>();
        cloudResources.add(createSensorResource("1000"));
        cloudResources.add(createActuatorResource("2000"));
        cloudResources.add(createServiceResource("3000"));

        // waiting for registrationHandler to create exchange
        int i = 1;
        while(i < 10) {
            try {
                LOG.debug("Atempting to register resources count {}.", i);
                rhClientService.registerResources(cloudResources);
                LOG.debug("Resources registered");
                break;
            } catch (Exception e) {
                i++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
            }
        }
    }
    
    private CloudResource createSensorResource(String internalId) {
        CloudResource cloudResource = new CloudResource();
        cloudResource.setInternalId(internalId);
        cloudResource.setPluginId(props.getEnablerName());
        cloudResource.setCloudMonitoringHost("cloudMonitoringHostIP");

        StationarySensor sensor = new StationarySensor();
        cloudResource.setResource(sensor);
        sensor.setLabels(Arrays.asList("termometer"));
        sensor.setComments(Arrays.asList("A comment"));
        sensor.setInterworkingServiceURL("https://symbiote-h2020.eu/example/interworkingService/");
        sensor.setLocatedAt(new WGS84Location(2.349014, 48.864716, 15, 
                Arrays.asList("Paris"), 
                Arrays.asList("This is Paris")));
        FeatureOfInterest featureOfInterest = new FeatureOfInterest();
        sensor.setFeatureOfInterest(featureOfInterest);
        featureOfInterest.setLabels(Arrays.asList("Room1"));
        featureOfInterest.setComments(Arrays.asList("This is room 1"));
        featureOfInterest.setHasProperty(Arrays.asList("temperature"));
        sensor.setObservesProperty(Arrays.asList("temperature,humidity".split(",")));
        
        CloudResourceParams cloudResourceParams = new CloudResourceParams();
        cloudResource.setParams(cloudResourceParams);
        cloudResourceParams.setType("Type of device, used in monitoring");

        return cloudResource;
    }

    private CloudResource createActuatorResource(String internalId) {
        CloudResource cloudResource = new CloudResource();
        cloudResource.setInternalId(internalId);
        cloudResource.setPluginId(props.getEnablerName());
        cloudResource.setCloudMonitoringHost("cloudMonitoringHostIP");
        
        Actuator actuator = new Actuator();
        cloudResource.setResource(actuator);
        actuator.setLabels(Arrays.asList("lamp"));
        actuator.setComments(Arrays.asList("A comment"));
        actuator.setInterworkingServiceURL("https://symbiote-h2020.eu/example/interworkingService/");
        actuator.setLocatedAt(new WGS84Location(2.349014, 48.864716, 15, 
                Arrays.asList("Paris"), 
                Arrays.asList("This is Paris")));
        
        Capability capability = new Capability();
        actuator.setCapabilities(Arrays.asList(capability));
        Effect effect = new Effect();
        capability.setEffects(Arrays.asList(effect));
        FeatureOfInterest featureOfInterest = new FeatureOfInterest();
        effect.setActsOn(featureOfInterest);
        Parameter parameter = new Parameter();
        capability.setParameters(Arrays.asList(parameter));
        parameter.setMandatory(true);
        parameter.setName("light");
        EnumRestriction enumRestriction = new EnumRestriction();
        enumRestriction.setValues(Arrays.asList("on", "off"));
        parameter.setRestrictions(Arrays.asList(enumRestriction));

        return cloudResource;
    }
    
    private CloudResource createServiceResource(String internalId) {
        CloudResource cloudResource = new CloudResource();
        cloudResource.setInternalId(internalId);
        cloudResource.setPluginId(props.getEnablerName());
        cloudResource.setCloudMonitoringHost("cloudMonitoringHostIP");
        
        Service service = new Service();
        cloudResource.setResource(service);
        service.setLabels(Arrays.asList("lamp"));
        service.setComments(Arrays.asList("A comment"));
        service.setInterworkingServiceURL("https://symbiote-h2020.eu/example/interworkingService/");
        
        service.setName("Heat alarm");
        Parameter parameter = new Parameter();
        parameter.setMandatory(true);
        parameter.setName("trasholdTemperature");
        service.setParameters(Arrays.asList(parameter));

        return cloudResource;
    }
```

### 7. Registering RAP plugin consumers

There are following RAP plugin consumers:
- for reading resources there is `ReadingResourceListener`
- for activating actuator and calling service there is `WritingToResourceListener`
- for beginning and ending subscription there is `NotificationResourceListener`

Registering and unregistering resources is done by calling `register...` or `unregister...` methods
in `RapPlugin` class. `RapPlugin`class can be injected in `ProcessingLogic` 
implementation like this:

```java
@Autowired
private RapPlugin rapPlugin;
```

#### Reading resources
`ReadingResourceListener` class has following methods:

- `List<Observation> readResource(String resourceId)` for reading one resource.
The argument is internal resource ID. It returns list of observed values.
- `List<Observation> readResourceHistory(String resourceId)` for reading
historical observed values which are returned.

In the case that reading is not possible listener should return `null`.

Here is example of registering and handling faked values:

```java
rapPlugin.registerReadingResourceListener(new ReadingResourceListener() {
    
    @Override
    public List<Observation> readResourceHistory(String resourceId) {
        if("1000".equals(resourceId))
            return new ArrayList<>(Arrays.asList(createObservation(resourceId), createObservation(resourceId)));

        return null;
    }
    
    @Override
    public List<Observation> readResource(String resourceId) {
        if("1000".equals(resourceId)) {
            Observation o = createObservation(resourceId);
            return new ArrayList<>(Arrays.asList(o));
        }
            
        return null;
    }
});
```
 
#### Triggering actuator and calling service
For both actions is used the same listener `WritingToResourceListener`. There 
is only one method in interface: 
`Result<Object> writeResource(String resourceId, List<InputParameter> parameters)`.
Arguments are: internal resource id and service/actuation parameters.
Parameters are implemented in `InputParameter` class. Return value is different
for actuation and service call:
- actuation - `null` is usual value, but it can be `Result` with message.
- service call - must have return value that is put in `Result` object.

Here is example of both implementations of listener:
```java
rapPlugin.registerWritingToResourceListener(new WritingToResourceListener() {
    
    @Override
    public Result<Object> writeResource(String resourceId, List<InputParameter> parameters) {
        LOG.debug("writing to resource {} body:{}", resourceId, parameters);
        if("2000".equals(resourceId)) { // actuation
            Optional<InputParameter> lightParameter = parameters.stream().filter(p -> p.getName().equals("light")).findFirst();
            if(lightParameter.isPresent()) {
                String value = lightParameter.get().getValue();
                if("on".equals(value)) {
                    LOG.debug("Turning on light {}", resourceId);
                    return new Result<>(false, null, "Turning on light " + resourceId);
                } else if("off".equals(value)) {
                    LOG.debug("Turning off light {}", resourceId);
                    return new Result<>(false, null, "Turning off light " + resourceId);
                }
            }
        } else if("3000".equals(resourceId)) { // service call
            Optional<InputParameter> lightParameter = parameters.stream().filter(p -> p.getName().equals("trasholdTemperature")).findFirst();
            if(lightParameter.isPresent()) {
                String value = lightParameter.get().getValue();
                LOG.debug("Setting trashold on resource {} to {}", resourceId, value);
                return new Result<>(false, null, "Setting trashold on resource " + resourceId + " to " + value);
                }
            }
            return null;
        }
    });
}
```

## Running

You can run this enabler as any other spring boot application.

`./gradlew bootRun`

or

`java -jar build/libs/EnablerLogicExample-0.0.1-SNAPSHOT.jar`

Note: In order to function correctly you need to start following components before: 
RabbitMQ server, Config Server, Eureka and Zipkin.
