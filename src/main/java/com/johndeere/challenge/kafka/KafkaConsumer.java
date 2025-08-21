package com.johndeere.challenge.kafka;

import com.johndeere.challenge.model.Message;
import com.johndeere.challenge.service.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class KafkaConsumer {

    private final MessageService service;
    private final KafkaProducer producer;

    @KafkaListener(topics = "inbound_message_queue", groupId = "johndeere")
    public void consume(Message message) {
        if (message == null) {
            log.warn("Received null message, skipping processing");
            return;
        }

        log.debug("Message received: sessionGuid={}, machineId={}",
                message.sessionGuid(), message.machineId());

        try {
            boolean forward = service.consumeMessage(message);

            if (forward) {
                producer.sendToKafka(message, "outbound_message_queue");
                log.debug("Message forwarded to outbound queue: sessionGuid={}",
                        message.sessionGuid());
            }
        } catch (Exception e) {
            log.error("Error processing message with sessionGuid={}: {}",
                    message.sessionGuid(), e.getMessage(), e);
        }
    }
}