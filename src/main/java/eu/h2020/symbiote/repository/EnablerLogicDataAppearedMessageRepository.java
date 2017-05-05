package eu.h2020.symbiote.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;

public interface EnablerLogicDataAppearedMessageRepository extends MongoRepository<EnablerLogicDataAppearedMessage,String>{

}
