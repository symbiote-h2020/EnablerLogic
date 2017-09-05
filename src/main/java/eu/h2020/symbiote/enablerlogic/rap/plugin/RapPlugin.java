/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.enablerlogic.rap.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.enablerlogic.messaging.RabbitManager;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessUnSubscribeMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.registration.RegisterPluginMessage;
import eu.h2020.symbiote.enablerlogic.rap.resources.RapDefinitions;
import eu.h2020.symbiote.enablerlogic.rap.resources.db.ResourceInfo;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

/**
 * This is class that handles requests from DSI/RAP.
 * 
 * @author Matteo Pardi <m.pardi@nextworks.it>
 * @author Mario Kušek <mario.kusek@fer.hr>
 * 
 */
@Service
public class RapPlugin implements SmartLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(RapPlugin.class);

    private RabbitManager rabbitManager;

    private boolean running = false;

    private String platformId;

    private boolean filtersSupported;

    private boolean notificationsSupported;

    private ReadingResourceListener readingResourceListener;

    private WritingToResourceListener writingToResourceListener;

    private NotificationResourceListener notificationResourceListener;

    @Autowired
    public RapPlugin(RabbitManager rabbitManager, EnablerLogicProperties props) {
        this(rabbitManager, 
                props.getEnablerName(), 
                props.getPlugin().isFiltersSupported(), 
                props.getPlugin().isNotificationsSupported()
        );
    }  
    
    public RapPlugin(RabbitManager rabbitManager, String platformId, boolean filtersSupported,
                boolean notificationsSupported) {
        this.rabbitManager = rabbitManager;
        this.platformId = platformId;
        this.filtersSupported = filtersSupported;
        this.notificationsSupported = notificationsSupported;
    }

    // TODO
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue,
//            exchange = @Exchange(value = "#{enablerLogicProperties.enablerLogicExchange.name}", type = "topic"),
//            key = "#{enablerLogicProperties.key.enablerLogic.dataAppeared}"
//        ))
    // klasa koja šalje je ResourceAccessRestController
    public String receiveMessage(String message) {
        String json = null;
        try {            
            ObjectMapper mapper = new ObjectMapper();
            ResourceAccessMessage msg = mapper.readValue(message, ResourceAccessMessage.class);
            ResourceAccessMessage.AccessType access = msg.getAccessType();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            switch(access) {
                case GET: {
                    // ODATA GET https://myplatform.eu:8102/rap/Sensors('symbioteId')/Observations? $top=1
                    // GET https://myplatform.eu:8102/rap/Sensor/{symbioteId}
                    ResourceAccessGetMessage msgGet = (ResourceAccessGetMessage) msg;
                    List<ResourceInfo> resInfoList = msgGet.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    List<Observation> observationLst = doReadResource(internalId);
                    json = mapper.writeValueAsString(observationLst);
                    break;
                }
                case HISTORY: {
                    // ODATA GET https://myplatform.eu:8102/rap/Sensors('symbioteId')/Observations
                    /*
                     Historical readings can be filtered, using the option $filter. Operators supported:
                        Equals, Not Equals, Less Than, Greater Than, And, Or
                     */
                    // GET https://myplatform.eu:8102/rap/Sensor/{symbioteId}/history
                    ResourceAccessHistoryMessage msgHistory = (ResourceAccessHistoryMessage) msg;
                    List<ResourceInfo> resInfoList = msgHistory.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    List<Observation> observationLst = doReadResourceHistory(internalId);
                    json = mapper.writeValueAsString(observationLst);       
                    break;
                }
                case SET: {
                    // ODATA PUT https://myplatform.eu:8102/rap/ActuatingServices(‘serviceId')
                    // POST https://myplatform.eu:8102/rap/Service(‘symbioteId')
                    /*
                       {
                            "inputParameters":
                            [  
                                { 
                                     "name": “prop1Name",
                                     "value": “prop1Value“
                                },
                                {
                                      "name": “prop2Name",
                                      "value": “prop2Value“
                                },
                                …
                            ]
                        } 
                     */
                    ResourceAccessSetMessage msgSet = (ResourceAccessSetMessage)msg;
                    List<ResourceInfo> resInfoList = msgSet.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    doWriteResource(internalId, msgSet.getBody());
                    break;
                }
                case SUBSCRIBE: {
                    // WebSocketController
                    ResourceAccessSubscribeMessage mess = (ResourceAccessSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
                        doSubscribeResource(info.getInternalId());
                    }
                    break;
                }
                case UNSUBSCRIBE: {
                    // WebSocketController
                    ResourceAccessUnSubscribeMessage mess = (ResourceAccessUnSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
                        doUnsubscribeResource(info.getInternalId());
                    }
                    break;
                }
                default:
                    throw new Exception("Access type " + access.toString() + " not supported");
            }
        } catch (Exception e) {
            LOG.error("Error while processing message:\n" + message + "\n" + e);
        }
        return json;
    }
    
    private void registerPlugin(String platformId, boolean hasFilters, boolean hasNotifications) {
        try {
            RegisterPluginMessage msg = new RegisterPluginMessage(platformId, hasFilters, hasNotifications);

            rabbitManager.sendMessage(RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_IN, 
                    RapDefinitions.PLUGIN_REGISTRATION_KEY, msg);
        } catch (Exception e ) {
            LOG.error("Error while registering plugin for platform " + platformId + "\n" + e);
        }
    }
    
    public void registerReadingResourceListener(ReadingResourceListener listener) {
        this.readingResourceListener = listener;
    }

    public void unregisterReadingResourceListener(ReadingResourceListener listener) {
        this.readingResourceListener = null;
    }

    public List<Observation> doReadResource(String resourceId) {
        if(readingResourceListener == null)
            throw new RuntimeException("ReadingResourceListener not registered in RapPlugin");
                    
        return readingResourceListener.readResource(resourceId);
    }
    
    // TODO test
    public List<Observation> doReadResourceHistory(String resourceId) {
        if(readingResourceListener == null)
            throw new RuntimeException("ReadingResourceListener not registered in RapPlugin");
                    
        return readingResourceListener.readResourceHistory(resourceId);
    }
    
    
    public void registerWritingToResourceListener(WritingToResourceListener listener) {
        this.writingToResourceListener = listener;
    }

    public void unregisterWritingToResourceListener(WritingToResourceListener listener) {
        this.writingToResourceListener = null;
    }

    // TODO test
    public void doWriteResource(String resourceId, String body) {
        if(writingToResourceListener == null)
            throw new RuntimeException("WritingToResourceListener not registered in RapPlugin");
                    
        writingToResourceListener.writeResource(resourceId, body);
    }

    public void registerNotificationResourceListener(NotificationResourceListener listener) {
        this.notificationResourceListener = listener;
    }

    public void unregisterNotificationResourceListener(NotificationResourceListener listener) {
        this.notificationResourceListener = null;
    }

    // TODO test    
    public void doSubscribeResource(String resourceId) {
        if(notificationResourceListener == null)
            throw new RuntimeException("NotificationResourceListener not registered in RapPlugin");
                    
        notificationResourceListener.subscribeResource(resourceId);
    }
    
    // TODO test
    public void doUnsubscribeResource(String resourceId) {
        if(notificationResourceListener == null)
            throw new RuntimeException("NotificationResourceListener not registered in RapPlugin");
                    
        notificationResourceListener.unsubscribeResource(resourceId);
    }
    
    // SmartLifecycle methods
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        running = false;
        callback.run();
    }

    @Override
    public void start() {
        registerPlugin(platformId, filtersSupported, notificationsSupported);
    }

    @Override
    public void stop() {
        // TODO unregisterPlugin()
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
