[![Build Status](https://api.travis-ci.org/symbiote-h2020/EnablerLogic.svg?branch=develop)](https://api.travis-ci.org/symbiote-h2020/EnablerLogic)
[![codecov.io](https://codecov.io/github/symbiote-h2020/EnablerLogic/branch/staging/graph/badge.svg)](https://codecov.io/github/symbiote-h2020/EnablerLogic)
[![](https://jitpack.io/v/symbiote-h2020/EnablerLogic.svg)](https://jitpack.io/#symbiote-h2020/EnablerLogic)

# EnablerLogic

## Using Generic EnablerLogic

The idea of Generic EnablerLogic is to use it as dependency in specific EnablerLogic. 
Generic parts like RabbitMQ communication with other components in enabler (e.g. ResourceManager, 
PlatformProxy, ...) are implemented in Generic EnablerLogic. That way a developer of specific enabler 
doesn't have to implement complex communication between those components. The example from this tutorial is in the following repository
[https://github.com/symbiote-h2020/EnablerLogicExample](https://github.com/symbiote-h2020/EnablerLogicExample).

## Creating specific EnablerLogic

### 1. Creating new SpringBoot project

It needs following dependencies: Config Client, Eureka Discovery, Zipkin Client

Current version of SpringBoot that is used is: **1.5.10**.

### 2. Adding symbIoTe dependencies to `build.gradle`

Add following dependencies for on the edge version:

`compile('com.github.symbiote-h2020:EnablerLogic:develop-SNAPSHOT') { changing = true }`

This is dependency to development version of EnablerLogic from jitpack. It will use the newest version of 
EnablerLogic published in jitpack. This is only for development. 

If you want to use stable version please use releases. Current release is 0.4.0 and you can include it with this line:

`compile('com.github.symbiote-h2020:EnablerLogic:0.4.0')` 

In order to use jitpack you need to put in `build.gradle` 
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
If you are running it in some other machine change this line accordingly.

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
public class ExampleLogic implements ProcessingLogic {
	private static final Logger log = LoggerFactory.getLogger(ExampleLogic.class);

	private EnablerLogic enablerLogic;

	@Override
	public void initialization(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;

		// put all initialization here
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
This method has variant with timeout.

- `public CancelTaskResponse cancelTask(CancelTaskRequest request)` -
this method sends to Resource Manager to cancel acquisition task request.
This method has variant with timeout.

- `public ResourceManagerUpdateResponse updateTask(ResourceManagerUpdateRequest request)` -
this method sends to Resource Manager to request for update acquisition task.
This method has variant with timeout.

- `public void reportBrokenResource(ProblematicResourcesMessage message)` -
this method sends message to Resource Manager that specified broken resource 
is producing wrong data.

Default timeout is 20 seconds.

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

* `int timeout` - this is optional parameter. It sets timeout for receiving 
response. The value is in milliseconds. If you don't specify timeout default
value is 20000 (20 seconds).


If the type of the response message can not be casted to clazz then 
`WrongResponseException` is thrown. The exception will contain the object that is returned.

In the case of timeout `null` will be returned.

Example without explicit timeout:
```java
MessageResponse response = enablerLogic.sendSyncMessageToEnablerLogic(
    "EnablerLogicInterpolator",
    new MessageRequest("request"),
    MessageResponse.class);
```

Example with explicit timeout:
```java
MessageResponse response = enablerLogic.sendSyncMessageToEnablerLogic(
    "EnablerLogicInterpolator",
    new MessageRequest("request"),
    MessageResponse.class,
    60_000);
```
### 6. Registering resources

Registration of resources is going by communicating with Registration Handler
component. This communication is REST based. For getting URL Eureka discovery
server is used. In the enabler spring boot application discovery client 
need to be enabled by putting `@EnableDiscoveryClient` in configuration:

```java
@SpringBootApplication
@EnableDiscoveryClient
public class EnablerLogicExample {

    public static void main(String[] args) {
		SpringApplication.run(EnablerLogicExample.class, args);
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
that are changed by this method (based on registration).

In the `CloudResoure` class should be put plugin id. The plugin id
can be obtained from  `EnablerLogicProperties` object that can be
injected. The method `getEnablerName()` returns plugin id.

Here is example of registration:
```java
public class ExampleLogic implements ProcessingLogic {
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
        cloudResources.add(createSensorResource("el_isen1"));
        cloudResources.add(createActuatorResource("el_iaid1"));
        cloudResources.add(createServiceResource("el_isrid1"));

        // waiting for registrationHandler to create exchange
        int i = 1;
        while(i < 10) {
            try {
                LOG.debug("Attempting to register resources count {}.", i);
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
    
    private void registerResources() {
        List<CloudResource> cloudResources = new LinkedList<>();
        cloudResources.add(createSensorResource("el_isen1"));
        cloudResources.add(createActuatorResource("el_iaid1"));
        cloudResources.add(createServiceResource("el_isrid1"));

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

        try {
			cloudResource.setAccessPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
			cloudResource.setFilteringPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		}
        
        StationarySensor sensor = new StationarySensor();
        cloudResource.setResource(sensor);
        sensor.setName("DefaultEnablerLogicSensor" + internalId);
        sensor.setDescription(Arrays.asList("Default sensor for testing EL"));

        FeatureOfInterest featureOfInterest = new FeatureOfInterest();
        sensor.setFeatureOfInterest(featureOfInterest);
        featureOfInterest.setName("outside air");
        featureOfInterest.setDescription(Arrays.asList("outside air quality"));
        featureOfInterest.setHasProperty(Arrays.asList("temperature,humidity".split(",")));
        
        sensor.setObservesProperty(Arrays.asList("temperature,humidity".split(",")));
        sensor.setLocatedAt(createLocation());
        sensor.setInterworkingServiceURL(props.getInterworkingInterfaceUrl());
        return cloudResource;        
    }

	private WGS84Location createLocation() {
		WGS84Location location = new WGS84Location(2.349014, 48.864716, 15, 
                "Paris", 
                Arrays.asList("This is Paris"));
		return location;
	}

    private CloudResource createActuatorResource(String internalId) {
        CloudResource cloudResource = new CloudResource();
        cloudResource.setInternalId(internalId);
        cloudResource.setPluginId(props.getEnablerName());
        
        try {
			cloudResource.setAccessPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
			cloudResource.setFilteringPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		}
        
        Actuator actuator = new Actuator();
        cloudResource.setResource(actuator);
        
        actuator.setLocatedAt(createLocation());
        actuator.setName("Enabler_Logic_Example_Aircondition_1");
        actuator.setDescription(Arrays.asList("This is aircondition 1"));
        
        eu.h2020.symbiote.model.cim.Capability capability = new eu.h2020.symbiote.model.cim.Capability();
        actuator.setCapabilities(Arrays.asList(capability));
        
        capability.setName("OnOffCapabililty");

        // parameters
        eu.h2020.symbiote.model.cim.Parameter parameter = new eu.h2020.symbiote.model.cim.Parameter();
        capability.setParameters(Arrays.asList(parameter));
        parameter.setName("on");
        parameter.setMandatory(true);
        PrimitiveDatatype datatype = new PrimitiveDatatype();
		parameter.setDatatype(datatype);
		datatype.setBaseDatatype("boolean");
        
        actuator.setInterworkingServiceURL(props.getInterworkingInterfaceUrl());

        return cloudResource;
    }
    
    private CloudResource createServiceResource(String internalId) {
        CloudResource cloudResource = new CloudResource();
        cloudResource.setInternalId(internalId);
        cloudResource.setPluginId(props.getEnablerName());
        
        try {
			cloudResource.setAccessPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
			cloudResource.setFilteringPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		}

        Service service = new Service();
        cloudResource.setResource(service);
        
        service.setName("Enabler_Logic_Example_Humidity_service_1");
        service.setDescription(Arrays.asList("This is humidity service 1 in Enabler Logic Example"));
        
        eu.h2020.symbiote.model.cim.Parameter parameter = new eu.h2020.symbiote.model.cim.Parameter();
        service.setParameters(Arrays.asList(parameter));

        parameter.setName("humidityTaget");
        parameter.setMandatory(true);
        // restriction
        LengthRestriction restriction = new LengthRestriction();
        restriction.setMin(2);
        restriction.setMax(10);
		parameter.setRestrictions(Arrays.asList(restriction));
		
		PrimitiveDatatype datatype = new PrimitiveDatatype();
		datatype.setArray(false);
		datatype.setBaseDatatype("http://www.w3.org/2001/XMLSchema#string");
		parameter.setDatatype(datatype);

        service.setInterworkingServiceURL(props.getInterworkingInterfaceUrl());

        return cloudResource;
    }
```

### 7. Registering RAP plugin consumers
RAP plugin consumers are called when symbIoTe Application access resources 
that enabler provides (registered resources). Since enabler can register
sensors, actuators and services there are corresponding RAP plugin consumers:
- for reading resources (sensors) there is `ReadingResourceListener`
- for triggering actuator there is `ActuatingResourceListener`
- for invoking service there is `InvokingServiceListener`
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

- `Observation readResource(String resourceId)` for reading one resource.
The argument is internal resource ID. It returns one observations.
- `List<Observation> readResourceHistory(String resourceId)` for reading
historical observed values which are returned.

In the case that reading is not possible to read listener should either return `null` or
throw `RapPluginException`.

Here is example of registering and handling faked values:

```java
rapPlugin.registerReadingResourceListener(new ReadingResourceListener() {
    
    @Override
    public List<Observation> readResourceHistory(String resourceId) {
        if("el_isen1".equals(resourceId))
            return new ArrayList<>(Arrays.asList(createObservation(resourceId), createObservation(resourceId)));

        return null;
    }
    
    @Override
    public Observation readResource(String resourceId) {
        if("el_isen1".equals(resourceId)) {
           return createObservation(resourceId);
        }
            
        return null;
    }
});
```

Here is example of creating observation:
```java
public Observation createObservation(String sensorId) {        
    Location loc = createLocation();
    
    TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    dateFormat.setTimeZone(zoneUTC);
    Date date = new Date();
    String timestamp = dateFormat.format(date);
    
    long ms = date.getTime() - 1000;
    date.setTime(ms);
    String samplet = dateFormat.format(date);
    
    ArrayList<ObservationValue> obsList = new ArrayList<>();
    ObservationValue obsval = 
            new ObservationValue(
                    Integer.toString(new Random().nextInt(50) - 10), // random temp. 
                    new Property("Temperature", "temp_iri", Arrays.asList("Air temperature")), 
                    new UnitOfMeasurement("C", "degree Celsius", "celsius_iri", Arrays.asList("")));
    obsList.add(obsval);
    
    obsval = new ObservationValue(
    		Integer.toString(new Random().nextInt(50) - 10), // random temp. 
            new Property("Humidity", "humidity_iri", Arrays.asList("Air humidity")), 
            new UnitOfMeasurement("C", "degree Celsius", "celsius_iri", Arrays.asList("")));
    obsList.add(obsval);
    
    Observation obs = new Observation(sensorId, loc, timestamp, samplet , obsList);
    
    try {
        LOG.debug("Observation: \n{}", new ObjectMapper().writeValueAsString(obs));
    } catch (JsonProcessingException e) {
        LOG.error("Can not convert observation to JSON", e);
    }
    
    return obs;
}
```

#### Triggering actuator
For triggering actuator is used `ActuatingResourceListener`. There 
is only one method in interface: 
`void actuateResource(String resourceId, Map<String,Capability> capabilities)`.
Arguments are: internal resource id and actuation parameters.
Capabilities are map with name of capability as key and 
value of capability implemented in `Capability` class as value.
Each capability has map of parameters (key is parameter name and value is implemented
in `Parameter` class). Each parameter has value.

Here is example of listener implementation:
```java
rapPlugin.registerActuatingResourceListener(new ActuatingResourceListener() {
	
	@Override
	public void actuateResource(String resourceId, Map<String,Capability> parameters) {
        LOG.debug("writing to resource {} body:{}", resourceId, parameters);
        try {
            if("el_iaid1".equals(resourceId)) {
                Capability lightCapability = parameters.get("OnOffCapability");
                Assert.notNull(lightCapability, "Capability 'on' is required.");
                Parameter parameter = lightCapability.getParameters().get("on");
                Assert.notNull(parameter, "Parameter 'on' in capability 'OnOffCapability' is required.");
                Object objectValue = parameter.getValue();
                Assert.isInstanceOf(Boolean.class, objectValue, "Parameter 'on' of capability 'OnOffCapability' should be boolean.");
                if((Boolean) objectValue) {
                    LOG.debug("*** Turning on AC {}", resourceId);
                } else {
                    LOG.debug("*** Turning off AC {}", resourceId);
                }
            } else {
            	throw new RapPluginException(HttpStatus.NOT_FOUND.value(), "Actuator not found");
            }
        } catch (IllegalArgumentException e) {
        	throw new RapPluginException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }
});
```

If there are problems in triggering actuation or in getting values from capabilities, 
implementation can throw `RapPluginException` with description and response 
code that will be returned to client.

#### Invoking service
For invoking service is used `InvokingServiceListener`. There 
is only one method in interface: 
`Object invokeService(String resourceId, Map<String,Parameter> parameters)`.
Arguments are: internal resource id and map of invocation parameters.
Parameters are similar to parameters in capabilities from actuation.
A map of invocation parameters has parameter name  as key and `Parameter` class
instance as value. Each parameter has concrete value.

Return value is result of invoking service. It is important that return value 
can be serialized to JSON. Internally is used Jackson for serialization. 

Here is example of listener implementation:
```java
rapPlugin.registerInvokingServiceListener(new InvokingServiceListener() {
	
	@Override
	public Object invokeService(String resourceId, Map<String,Parameter> parameters) {
        LOG.debug("invoking service {} parameters:{}", resourceId, parameters);
        
        try {
            if("el_isrid1".equals(resourceId)) {
            	Parameter parameter = parameters.get("humidityTaget");
                Assert.notNull(parameter, "Capability 'humidityTaget' is required.");
                Object objectValue = parameter.getValue();
                Assert.isInstanceOf(String.class, objectValue, "Parameter 'humidityTaget' should be string of length form 2-10.");
                String value = (String) objectValue;
                LOG.debug("Invoking service {} with param {}.", resourceId, value);
                LOG.info("*** Humidity service target is {}", value);
                return "ok";
            } else {
            	throw new RapPluginException(HttpStatus.NOT_FOUND.value(), "Service not found");
            }
        } catch (IllegalArgumentException e) {
        	throw new RapPluginException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
	}
});
```

The same as for actuators, if there are problems in invoking service or in getting values 
from parameters, implementation can throw `RapPluginException` with description and 
response code that will be returned to client.

### 8. One time access to resources
Sometimes there is need to access resources of other platforms just once.
The steps are following:
* Search for resource
* Send access request

The example that will show how to once access resource is: when data for
reading temperature and humidity appears then the temperature and humidity
logic is executed. Temperature logic is as following if the temperature is
grater then 25C it will actuate resource and by that turn on air condition (AC).
Similar is for humidity. If humidity is in acceptable range (30-60%) humidity
service target will be OFF and otherwise it will be set to 40%.  

Here is implementation of starting logic:
```java
@Override
public void measurementReceived(EnablerLogicDataAppearedMessage dataAppeared) {
    try {
        LOG.info("Received new Observations: {}", new ObjectMapper().writeValueAsString(dataAppeared));
    } catch (JsonProcessingException e) {
        LOG.error("Problem with deserializing EnablerLogicDataAppearedMessage", e);
    }
    
    temperatureLogic(dataAppeared);
    humidityLogic(dataAppeared);
}

private void temperatureLogic(EnablerLogicDataAppearedMessage dataAppeared) {
	try {
		LOG.info("Logic for Paris temp");
		dataAppeared.getObservations().get(0).getObsValues().stream()
			.filter(obsValue -> obsValue.getObsProperty().getName().equalsIgnoreCase("temperature"))
			.map(obsValue -> obsValue.getValue())
			.forEach(tempValue -> {
				if(Integer.parseInt(tempValue) > 25)
					actuateAirCondition(true);
				else
					actuateAirCondition(false);
			});
    } catch (Exception e) {
    	LOG.error("Error in logic for Paris temp", e);
    }
}

private void humidityLogic(EnablerLogicDataAppearedMessage dataAppeared) {
	try {
		LOG.info("Logic for Paris humidity");
		dataAppeared.getObservations().get(0).getObsValues().stream()
			.filter(obsValue -> obsValue.getObsProperty().getName().equalsIgnoreCase("humidity"))
			.map(obsValue -> obsValue.getValue())
			.forEach(tempValue -> {
				int humidity = Integer.parseInt(tempValue);
				if(humidity > 60 || humidity < 30)
					turnOnHumidityService(40);
				else
					turnOffHumidityService();
			});
    } catch (Exception e) {
    	LOG.error("Error in logic for Paris temp", e);
    }
}

private void turnOffHumidityService() {
	LOG.info("Turning OFF service");
	setHumidityServiceTarget("OFF");
}

private void turnOnHumidityService(int targetHumidity) {
	LOG.info("Setting humidiy service to target {}.", targetHumidity);
	setHumidityServiceTarget(String.valueOf(targetHumidity));
}
```
#### Search for resource

For searching resources we need to send request to Resource Manager. 
That is done by using `queryResourceManager` method. This method has 
`ResourceManagerTaskInfoRequest` parameter. In the `ResourceManagerTaskInfoRequest`
we need to set:
* taskId - the id of the requested task (specified name)
* minNoResources - the minimum number of required resources (we set that to `1`)
* coreQueryRequest - the request which is propagated to the core. We need to set search criteria here (only the name of resource).
* queryInterval - the query interval in ISO-8601 alternateExtended format 
that is propagated to the Platform Proxy (`null` since this is not going to 
be propagated to Platfrom Proxy)
* allowCaching - if the results gotten from search are allowed to be cached 
for faster responses in case of failing resources (`false` this is on time search)
* cachingInterval - the caching interval of tasks resources in ISO-8601 
alternateExtended format (`null` this is one time search)
* informPlatformProxy - if Platform Proxy needs to be informed. If you want 
to receive back data set to true. Otherwise, if you just need to query the 
Core for getting back the resource descriptions, set to false (`false`)
* enablerLogicName - the enabler logic component which owns this task and 
it will receive updates for it (this is fetch from enabler properties)
* sparqlQueryRequest - the request in SPARQL. Set to null if you use 
CoreQueryRequest. If set overwrites the CoreQueryRequest (`null`)

In parenthesis are values that we use in example.

From search response we need to check if it is successful response. 

After that we need to extract task and from task one `ResourceManagerTaskInfoResponse`
and create `PlatformProxyResourceInfo` for sending to Platfrom Proxy.

Here is temperature example:
```java
private Optional<PlatformProxyResourceInfo> findAirConditionInfo() {
	CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
	coreQueryRequest.setName("Enabler_Logic_Example_Aircondition_1");

    ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest(
    		"airCondition", 1, 1, coreQueryRequest, 
    		null, //"P0000-00-00T00:01:00",
    		false, null, false, props.getEnablerName(), null);

    ResourceManagerAcquisitionStartResponse response = enablerLogic.queryResourceManager(request);

    try {
        LOG.info("Response JSON: {}", new ObjectMapper().writeValueAsString(response));
    } catch (JsonProcessingException e) {
        LOG.info("Response: {}", response);
    }
    
    if(response.getStatus() != ResourceManagerTasksStatus.SUCCESS) {
    	LOG.warn("Did not found air condition actuator!!!");
    	return Optional.empty();
    }
    	
    ResourceManagerTaskInfoResponse resourceManagerTaskInfoResponse = response.getTasks().get(0);
	String resourceId = resourceManagerTaskInfoResponse.getResourceDescriptions().get(0).getId();
	String accessURL = resourceManagerTaskInfoResponse.getResourceUrls().get(resourceId);
	
	PlatformProxyResourceInfo info = new PlatformProxyResourceInfo();
	info.setAccessURL(accessURL);
	info.setResourceId(resourceId);
	return Optional.of(info);
}
```

And here is humidity example:

```java
private Optional<PlatformProxyResourceInfo> findHumidityService() {
	CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
	coreQueryRequest.setName("Enabler_Logic_Example_Humidity_service_1");

    ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest(
    		"humidityService", 1, 1, coreQueryRequest, 
    		null, //"P0000-00-00T00:01:00",
    		false, null, false, props.getEnablerName(), null);

    ResourceManagerAcquisitionStartResponse response = enablerLogic.queryResourceManager(request);

    try {
        LOG.info("Response JSON: {}", new ObjectMapper().writeValueAsString(response));
    } catch (JsonProcessingException e) {
        LOG.info("Response: {}", response);
    }
    
    if(response.getStatus() != ResourceManagerTasksStatus.SUCCESS) {
    	LOG.warn("Did not found humidity service!!!");
    	return Optional.empty();
    }
    	
    ResourceManagerTaskInfoResponse resourceManagerTaskInfoResponse = response.getTasks().get(0);
	String resourceId = resourceManagerTaskInfoResponse.getResourceDescriptions().get(0).getId();
	String accessURL = resourceManagerTaskInfoResponse.getResourceUrls().get(resourceId);
	
	PlatformProxyResourceInfo info = new PlatformProxyResourceInfo();
	info.setAccessURL(accessURL);
	info.setResourceId(resourceId);
	return Optional.of(info);
}
```

#### Send access request

After getting `PlatformProxyResourceInfo` we need to send request to Platform
Proxy to execute access to that resource.

##### Actuating resource
Here is temperature example:
```java
private void actuateAirCondition(boolean state) {
	findAirConditionInfo().ifPresent(resource -> {
    	LOG.info("Actuating {} with state {}", resource.getResourceId(), state);
    	enablerLogic.triggerActuator(new ActuatorExecutionTaskInfo("triggerAirCondition", 
    			resource, props.getEnablerName(), "OnOffCapability", 
    			Arrays.asList(new ServiceParameter("on", state))));
	});
}
```

In this example we are using `triggerActuator` method. It has `ActuatorExecutionTaskInfo`
as parameter. When creating `ActuatorExecutionTaskInfo` we need following parameters:
* taskId - the id of the requested task (we specify that name)
* resource - this is found `PlatformProxyResourceInfo`
* enablerLogicName - enabler logic name from properties
* capabilityName - capability that will be triggered (`OnOffCapability`)
* parameters - list of parameters for that capability

The result of that method is `ServiceExecutionTaskResponse` object which
contains HTTP response and output which should be empty.

##### Invoking service
Here is humidity example:
```java
private void setHumidityServiceTarget(String target) {
	findHumidityService().ifPresent(resource -> {
    	LOG.info("Setting service {} to target {}", resource.getResourceId(), target);
    	enablerLogic.invokeService(new ServiceExecutionTaskInfo("humidityServiceTarget", 
    			resource, props.getEnablerName(),  
    			Arrays.asList(new ServiceParameter("humidityTaget", target))));
	});
}
```

When invoking service we use `invokeService` method with parameter of type 
`ServiceExecutionTaskInfo`. When creating `ServiceExecutionTaskInfo` we need:
* taskId - the id of the requested task (we specify that name)
* resource - this is found `PlatformProxyResourceInfo`
* enablerLogicName - enabler logic name from properties
* parameters - list of parameters for that service

This method will return `ServiceExecutionTaskResponse` object which has
HTTP status of invoking service and output (String) from service.

#####  Reading resource
For reading resource we are using `readResource` method. This method has
parameter object of type `PlatformProxyTaskInfo` and returns `EnablerLogicDataAppearedMessage` 
object. the resulting object contains list of observations from that sensor.

## Running

You can run this enabler as any other spring boot application.

`./gradlew bootRun`

or

`java -jar build/libs/EnablerLogicExample-0.0.1-SNAPSHOT.jar`

Note: In order to function correctly you need to start following components before: 
RabbitMQ server, Config Server, Eureka and Zipkin.
