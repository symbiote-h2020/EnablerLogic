package eu.h2020.symbiote.enablerlogic.rap.plugin;

public interface WritingToResourceListener {
    /**  
     * This method is called when DSI/RAP is received request for actuation.
     * In the implementation of this method put here a call to the platform 
     * with internal resource id and parameters for setting the actuator value.
     * 
     * @param resourceId internal resource id
     * @param body actuation parameters
     */
    void writeResource(String resourceId, String body);

}
