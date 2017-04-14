package eu.h2020.symbiote.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import eu.h2020.symbiote.model.AcquisitionTask;

@Repository
public interface AcquisitionTaskRepository extends MongoRepository<AcquisitionTask,String>{
}
