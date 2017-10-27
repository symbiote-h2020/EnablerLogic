package eu.h2020.symbiote.enablerlogic.rap.plugin;

import java.util.List;

import eu.h2020.symbiote.model.cim.Observation;

public interface ReadingResourceListener {
    /**  
     * This method is called when DSI/RAP is asking for resource data.
     * In implementation you should put the query to the platform with 
     * internal resourceId to get data.
     * 
     * @param resourceId internal resource id
     * @return list of observed values
     */
    List<Observation> readResource(String resourceId);
    
    /**
     * This method is called when DSI/RAP is asking for historical resource data.
     * In implementation you should put the query to the platform with internal 
     * resource id to get historical data.
     * 
     * @param resourceId internal resource id
     * @return list of historical observed values
     */
    List<Observation> readResourceHistory(String resourceId);
}
