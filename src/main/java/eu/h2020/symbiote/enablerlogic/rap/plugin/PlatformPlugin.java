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
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.access.ResourceAccessUnSubscribeMessage;
import eu.h2020.symbiote.enablerlogic.rap.messages.registration.RegisterPluginMessage;
import eu.h2020.symbiote.enablerlogic.rap.resources.RapDefinitions;
import eu.h2020.symbiote.enablerlogic.rap.resources.db.ResourceInfo;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 * @author Mario Kušek <mario.kusek@fer.hr>
 * 
 */
public abstract class PlatformPlugin implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(PlatformPlugin.class);

    private RabbitManager rabbitManager;

    private boolean running = false;

    private String platformId;

    private boolean hasFilters;

    private boolean hasNotifications;


    public PlatformPlugin(RabbitManager rabbitManager, String platformId, 
            boolean hasFilters, boolean hasNotifications) {
        this.rabbitManager = rabbitManager;
        this.platformId = platformId;
        this.hasFilters = hasFilters;
        this.hasNotifications = hasNotifications;
    }  
    
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue,
//            exchange = @Exchange(value = "#{enablerLogicProperties.enablerLogicExchange.name}", type = "topic"),
//            key = "#{enablerLogicProperties.key.enablerLogic.dataAppeared}"
//        ))
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
                    ResourceAccessGetMessage msgGet = (ResourceAccessGetMessage) msg;
                    List<ResourceInfo> resInfoList = msgGet.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    List<Observation> observationLst = readResource(internalId);
                    json = mapper.writeValueAsString(observationLst);
                    break;
                }
                case HISTORY: {
                    ResourceAccessHistoryMessage msgHistory = (ResourceAccessHistoryMessage) msg;
                    List<ResourceInfo> resInfoList = msgHistory.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    List<Observation> observationLst = readResourceHistory(internalId);
                    json = mapper.writeValueAsString(observationLst);       
                    break;
                }
                case SET: {
                    ResourceAccessSetMessage msgSet = (ResourceAccessSetMessage)msg;
                    List<ResourceInfo> resInfoList = msgSet.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    writeResource(internalId, msgSet.getBody());
                    break;
                }
                case SUBSCRIBE: {
                    ResourceAccessSubscribeMessage mess = (ResourceAccessSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
                        subscribeResource(info.getInternalId());
                    }
                    break;
                }
                case UNSUBSCRIBE: {
                    ResourceAccessUnSubscribeMessage mess = (ResourceAccessUnSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
                        unsubscribeResource(info.getInternalId());
                    }
                    break;
                }
                default:
                    throw new Exception("Access type " + access.toString() + " not supported");
            }
        } catch (Exception e) {
            log.error("Error while processing message:\n" + message + "\n" + e);
        }
        return json;
    }
    
    // TODO integration test for registering plugin
    private void registerPlugin(String platformId, boolean hasFilters, boolean hasNotifications) {
        try {
            RegisterPluginMessage msg = new RegisterPluginMessage(platformId, hasFilters, hasNotifications);

            rabbitManager.sendMessage(RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_IN, 
                    RapDefinitions.PLUGIN_REGISTRATION_KEY, msg);
        } catch (Exception e ) {
            log.error("Error while registering plugin for platform " + platformId + "\n" + e);
        }
    }
    
    /*  
    *   OVERRIDE this, inserting the query to the platform with internal resource id
    */
    public abstract List<Observation> readResource(String resourceId);
    
    /*  
    *   OVERRIDE this, inserting here a call to the platform with internal resource id
    *   setting the actuator value
    */
    public abstract void writeResource(String resourceId, String body);
        
    /*  
    *   OVERRIDE this, inserting the query to the platform with internal resource id
    */
    public abstract List<Observation> readResourceHistory(String resourceId);
    
    /*  
    *   OVERRIDE this, inserting the subscription of the resource
    */
    public abstract void subscribeResource(String resourceId);
    
    /*  
    *   OVERRIDE this, inserting the unsubscription of the resource
    */
    public abstract void unsubscribeResource(String resourceId);
    
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
        registerPlugin(platformId, hasFilters, hasNotifications);
        
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
