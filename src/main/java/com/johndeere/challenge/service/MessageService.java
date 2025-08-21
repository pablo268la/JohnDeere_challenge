package com.johndeere.challenge.service;

import com.johndeere.challenge.config.MachineConfig;
import com.johndeere.challenge.model.Message;
import com.johndeere.challenge.repository.MessageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.client.api.PetApi;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MachineConfig machineConfig;
    private final PetApi petApi;

    public boolean consumeMessage(Message message) {
        if (message == null) {
            log.warn("Received null message in service");
            return false;
        }

        log.debug("Processing message: sessionGuid={}, sequenceNumber={}, machineId={}",
                message.sessionGuid(), message.sequenceNumber(), message.machineId());

        boolean isAuthorized = checkMachineAuthorization(message);

        if (isAuthorized) {
            saveMessage(message);
            return true;
        } else {
            return false;
        }
    }

    private boolean checkMachineAuthorization(Message message) {
        // We are calling the PetApi to show how an external API would be used
        // Then we are checking if the machineId is in the whitelist

        try {
            var machine = petApi.getPetById((long) message.machineId());

            boolean isNotDuplicate = isMessageNotDuplicate(message);
            boolean isInWhitelist = machineConfig.getWhitelist().stream()
                    .anyMatch(id -> id == message.machineId());

            return isInWhitelist && isNotDuplicate;

        } catch (Exception e) {
            log.error("Error during machine authorization for machineId {}: {}",
                    message.machineId(), e.getMessage());
            return false;
        }
    }

    private boolean isMessageNotDuplicate(Message message) {
        Message existingMessage = getMessageById(message.sessionGuid());
        if (existingMessage == null) {
            return true;
        }

        boolean isDuplicate = existingMessage.sequenceNumber() == message.sequenceNumber();

        return !isDuplicate;
    }

    public void saveMessage(Message message) {
        try {
            messageRepository.save(message);
            log.debug("Message persisted: sessionGuid={}", message.sessionGuid());
        } catch (Exception e) {
            log.error("Error saving message with sessionGuid {}: {}",
                    message.sessionGuid(), e.getMessage(), e);
            throw new RuntimeException("Failed to persist message", e);
        }
    }

    public Message getMessageById(UUID id) {
        return messageRepository.findById(id).orElse(null);
    }
}