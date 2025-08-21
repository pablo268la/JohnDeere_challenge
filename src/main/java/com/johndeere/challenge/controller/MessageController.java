package com.johndeere.challenge.controller;

import com.johndeere.challenge.kafka.KafkaProducer;
import com.johndeere.challenge.model.Message;
import com.johndeere.challenge.model.dto.MessageDto;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/messages")
public class MessageController {

    private final KafkaProducer kafkaProducer;


    @PostMapping
    public ResponseEntity<Message> sendMessage(@RequestBody MessageDto messageDto,
                                               @RequestParam String topic) {
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(messageDto.sessionGuid())
                .sequenceNumber(messageDto.sequenceNumber())
                .machineId(messageDto.machineId())
                .data(messageDto.data())
                .build();
        kafkaProducer.sendToKafka(message, topic);
        return ResponseEntity.ok(message);
    }
}