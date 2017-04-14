package eu.h2020.symbiote.model;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.h2020.symbiote.core.model.Observation;

/**
 * AcquisitionTask abstraction model.
 *
 * Created by Petar Krivic.
 */
public class AcquisitionTask {
	
	@Id
	@JsonProperty("taskId")
	private String taskId;
	
	@JsonProperty("timestamp")
	private String timestamp;		//not sure if String or something else..
	
	@JsonProperty("resourceId")
	private String resourceId;
	
	@JsonProperty("data")			//as proposed in confluence, could be also observation..
	private Observation observation;
	
	public AcquisitionTask(@JsonProperty("taskId")String taskId,
				@JsonProperty("timestamp")String timestamp,
				@JsonProperty("resourceId")String resourceId, 
				@JsonProperty("data")Observation data){
		this.taskId = taskId;
		this.timestamp = timestamp;
		this.resourceId = resourceId;
		this.observation = data;
	}

	public String getTaskId() {
		return taskId;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getResourceId() {
		return resourceId;
	}

	public Observation getObservation() {
		return observation;
	}

	
}
