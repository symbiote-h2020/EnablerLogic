package eu.h2020.symbiote.enablerlogic.rap.plugin;

import eu.h2020.symbiote.cloud.model.data.Result;

public interface WritingToResourceListener {
    /**  
     * This method is called when DSI/RAP is received request for actuation.
     * In the implementation of this method put here a call to the platform 
     * with internal resource id and parameters for setting the actuator value.
     * 
     * @param resourceId internal resource id
     * @param body service/actuation parameters
     * @return service result, null if actuation is triggered
     */
    Result<Object> writeResource(String resourceId, String body);
}
