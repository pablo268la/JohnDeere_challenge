package com.johndeere.challenge.kafka;

import com.johndeere.challenge.model.Message;
import com.johndeere.challenge.model.MessageData;
import com.johndeere.challenge.model.MessageDataType;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Component
@AllArgsConstructor
public class    KafkaProducer {

    private final KafkaTemplate<String, Message> kafkaTemplate;

    @PostConstruct
    public void init() {
        log.info("KafkaProducer initialized");
        sendToKafka(Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(UUID.randomUUID())
                .sequenceNumber(1)
                .machineId(1)
                .data(List.of(
                                MessageData.builder()
                                        .type(MessageDataType.DISTANCE)
                                        .unit("m")
                                        .value("100")
                                        .build(),
                                MessageData.builder()
                                        .type(MessageDataType.WORKED_SURFACE)
                                        .unit("m2")
                                        .value("600")
                                        .build()
                        )
                )
                .build(), "inbound_message_queue");
        sendToKafka(Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(UUID.randomUUID())
                .sequenceNumber(2)
                .machineId(1)
                .data(List.of(
                                MessageData.builder()
                                        .type(MessageDataType.DISTANCE)
                                        .unit("m")
                                        .value("102")
                                        .build(),
                                MessageData.builder()
                                        .type(MessageDataType.WORKED_SURFACE)
                                        .unit("m2")
                                        .value("610")
                                        .build()
                        )
                )
                .build(), "inbound_message_queue");


    }

    public void sendToKafka(Message data, String topic) {
        final ProducerRecord<String, Message> record = new ProducerRecord<>(topic, data);
        CompletableFuture<SendResult<String, Message>> future = kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(result.getProducerRecord().topic() + " - " + result.getProducerRecord().value());
            } else {
                log.info(ex.toString());
            }
        });
    }
}