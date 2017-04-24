package eu.h2020.symbiote.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoResponse;

@Repository
public interface ResourceManagerTaskInfoResponseRepository extends MongoRepository<ResourceManagerTaskInfoResponse,String>{
	
	public ResourceManagerTaskInfoResponse findByTaskId(String TaskId);
}
