/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.enablerlogic.rap.plugin;

import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.cloud.model.data.observation.Property;
import eu.h2020.symbiote.cloud.model.data.observation.UnitOfMeasurement;
import eu.h2020.symbiote.enablerlogic.messaging.RabbitManager;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author Mario Kušek <mario.kusek@fer.hr>
 */
@Service
public class EnablerSpecificPlatformPlugin extends PlatformPlugin {
    
    private static final Logger LOG = LoggerFactory.getLogger(EnablerSpecificPlatformPlugin.class);
    
    private final String PLUGIN_PLATFORM_ID;
    private final String PLUGIN_RESOURCES_ACCESS_QUEUE;   

    public EnablerSpecificPlatformPlugin(RabbitManager rabbitManager, EnablerLogicProperties props) {
        super(rabbitManager, props.getEnablerName(), 
                props.getPlugin().isFiltersSupported(), 
                props.getPlugin().isNotificationsSupported());
        PLUGIN_PLATFORM_ID = props.getEnablerName();
        PLUGIN_RESOURCES_ACCESS_QUEUE = "rap-platform-queue_" + PLUGIN_PLATFORM_ID;
    }

    @Override
    public List<Observation> readResource(String resourceId) {
        LOG.debug("read resourceId: {}", resourceId);
        List<Observation> value = new ArrayList<>();
        //
        // INSERT HERE: query to the platform with internal resource id
        //
        // example
        Observation obs = observationExampleValue();
        value.add(obs);
        
        return value;
    }
    
    @Override
    public void writeResource(String resourceId, String body) {
        LOG.debug("write resourceId: {}, body {}", resourceId, body);
        // INSERT HERE: call to the platform with internal resource id
        // setting the actuator value
    }
    
    @Override
    public List<Observation> readResourceHistory(String resourceId) {
        LOG.debug("read history resourceId: {}", resourceId);
        List<Observation> value = new ArrayList<>();
        //
        // INSERT HERE: query to the platform with internal resource id
        //
        // example
        Observation obs1 = observationExampleValue();
        Observation obs2 = observationExampleValue();
        value.add(obs1);
        value.add(obs2);
        
        return value;
    }
    
    @Override
    public void subscribeResource(String resourceId) {
        LOG.debug("subscribe to resourceId: {}", resourceId);
        // INSERT HERE: call to the platform to subscribe resource
    }
    
    @Override
    public void unsubscribeResource(String resourceId) {
        LOG.debug("unsubscribe to resourceId: {}", resourceId);
        // INSERT HERE: call to the platform to unsubscribe resource
    }
    
    /* 
    *   Some sample code for observations 
    */   
    public Observation observationExampleValue () {        
        String sensorId = "symbIoTeID1";
        Location loc = new Location(15.9, 45.8, 145, "Spansko", "City of Zagreb");
        TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(zoneUTC);
        Date date = new Date();
        String timestamp = dateFormat.format(date);
        long ms = date.getTime() - 1000;
        date.setTime(ms);
        String samplet = dateFormat.format(date);
        eu.h2020.symbiote.cloud.model.data.observation.ObservationValue obsval = 
                new eu.h2020.symbiote.cloud.model.data.observation.ObservationValue("7", 
                        new Property("Temperature", "Air temperature"), 
                        new UnitOfMeasurement("C", "degree Celsius", ""));
        ArrayList<eu.h2020.symbiote.cloud.model.data.observation.ObservationValue> obsList = new ArrayList<>();
        obsList.add(obsval);
        Observation obs = new Observation(sensorId, loc, timestamp, samplet , obsList);
        
        LOG.debug("Observation: \n" + obs.toString());
        
        return obs;
    }
}
