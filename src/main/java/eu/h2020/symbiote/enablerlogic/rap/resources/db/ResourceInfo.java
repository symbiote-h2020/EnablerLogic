/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.enablerlogic.rap.resources.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceInfo {
    
    @Id
    @JsonProperty("symbioteId")
    private String id;
    @JsonProperty("internalId")
    private String internalId;
    @JsonProperty("type")
    private String type;
    @JsonProperty("observedProperties")
    private List<String> observedProperties;
    @JsonIgnore
    private List<String> sessionIdList;
    @JsonIgnore
    private String pluginId;
    
    
    public ResourceInfo() {
        this.id = "";
        this.internalId = "";
        this.pluginId = null;
        this.observedProperties = null;
        this.sessionIdList = null;
        this.type = null;
    }
    
    @JsonCreator
    public ResourceInfo(@JsonProperty("symbioteId") String resourceId, 
                        @JsonProperty("internalId") String platformResourceId) {
        this.id = resourceId;
        this.internalId = platformResourceId;
        this.pluginId = null;
        this.observedProperties = null;
        this.sessionIdList = null;       
        this.type = null;
    }
    
    @JsonProperty("symbioteId")
    public String getSymbioteId() {
        return id;
    }
    
    @JsonProperty("symbioteId")
    public void setSymbioteId(String symbioteId) {
        this.id = symbioteId;
    }
    
    @JsonProperty("internalId")
    public String getInternalId() {
        return internalId;
    }
    
    @JsonProperty("internalId")
    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }
    
    @JsonProperty("observedProperties")
    public List<String> getObservedProperties() {
        return observedProperties;
    }    
    
    @JsonProperty("observedProperties")
    public void setObservedProperties(List<String> observedProperties) {
        this.observedProperties = observedProperties;
    }
    
    @JsonProperty("type")
    public String getType() {
        return type;
    }
    
    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }
    
    @JsonIgnore
    public List<String> getSessionId() {
        return sessionIdList;
    }
    
    @JsonIgnore
    public void setSessionId(List<String> sessionIdList) {
        this.sessionIdList = sessionIdList;
    }
    
    @JsonIgnore
    public void addToSessionList(String sessionId) {
        if(this.sessionIdList == null)
            this.sessionIdList = new ArrayList<>();
        this.sessionIdList.add(sessionId);
    }
    
    @JsonIgnore
    public String getPluginId() {
        return pluginId;
    }
    
    @JsonIgnore
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }
}