package com.johndeere.challenge.repository;

import com.johndeere.challenge.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends MongoRepository<Message, UUID> {

    List<Message> findBySessionGuid(UUID sessionGuid);

}