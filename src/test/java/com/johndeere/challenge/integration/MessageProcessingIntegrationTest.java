package com.johndeere.challenge.integration;

import com.johndeere.challenge.model.Message;
import com.johndeere.challenge.model.MessageData;
import com.johndeere.challenge.model.MessageDataType;
import com.johndeere.challenge.repository.MessageRepository;
import com.johndeere.challenge.service.MessageService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MessageProcessingIntegrationTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:5.0").withReuse(false);

    @Container
    static ConfluentKafkaContainer kafkaContainer = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0").withReuse(false);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure MongoDB
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);

        // Configure Kafka
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafkaContainer::getBootstrapServers);

        // Configure machine whitelist for tests
        registry.add("machine.whitelist", () -> "1,2,3");
    }

    @Autowired
    private KafkaTemplate<String, Message> kafkaTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageService messageService;

    private CountDownLatch outboundLatch;
    private Message receivedOutboundMessage;
    private UUID currentTestSessionGuid; // Track current test message

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        outboundLatch = new CountDownLatch(1);
        receivedOutboundMessage = null;
        currentTestSessionGuid = null;
    }

    @Test
    void shouldProcessAuthorizedMessageAndForwardToOutboundQueue() throws InterruptedException {
        currentTestSessionGuid = UUID.randomUUID();
        Message testMessage = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(currentTestSessionGuid)
                .sequenceNumber(1)
                .machineId(1)
                .data(List.of(
                        MessageData.builder()
                                .type(MessageDataType.DISTANCE)
                                .unit("m")
                                .value("200")
                                .build(),
                        MessageData.builder()
                                .type(MessageDataType.WORKED_SURFACE)
                                .unit("m2")
                                .value("500")
                                .build()
                ))
                .build();

        kafkaTemplate.send("inbound_message_queue", testMessage);

        Thread.sleep(5000);

        assertNotNull(receivedOutboundMessage);
        assertEquals(currentTestSessionGuid, receivedOutboundMessage.sessionGuid());
        assertEquals(1, receivedOutboundMessage.sequenceNumber());
        assertEquals(1, receivedOutboundMessage.machineId());

        assertNotNull(messageService.getMessagesBySessionId(testMessage.sessionGuid()).getFirst(),
                "Message should be persisted in MongoDB");
    }

    @Test
    void shouldRejectUnauthorizedMachineAndNotForward() throws InterruptedException {
        currentTestSessionGuid = UUID.randomUUID();
        Message testMessage = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(currentTestSessionGuid)
                .sequenceNumber(1)
                .machineId(99)
                .data(List.of(
                        MessageData.builder()
                                .type(MessageDataType.DISTANCE)
                                .unit("m")
                                .value("100")
                                .build()
                ))
                .build();

        kafkaTemplate.send("inbound_message_queue", testMessage);

        assertFalse(messageRepository.findById(testMessage.id()).isPresent(),
                "Unauthorized message should NOT be persisted in MongoDB");
    }

    @Test
    void shouldRejectDuplicateSequenceNumberAndNotForward() throws InterruptedException {
        currentTestSessionGuid = UUID.randomUUID();
        Message firstMessage = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(currentTestSessionGuid)
                .sequenceNumber(1)
                .machineId(1)
                .data(List.of())
                .build();

        messageRepository.save(firstMessage);

        Message duplicateMessage = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(currentTestSessionGuid)
                .sequenceNumber(1) // Same sequence number
                .machineId(1)
                .data(List.of(
                        MessageData.builder()
                                .type(MessageDataType.DISTANCE)
                                .unit("m")
                                .value("200")
                                .build()
                ))
                .build();

        kafkaTemplate.send("inbound_message_queue", duplicateMessage);


        messageRepository.findAll().forEach(System.out::println);
        // Verify only original message exists in MongoDB
        var messages = messageRepository.findAll().stream().filter(m -> m.sessionGuid().equals(currentTestSessionGuid)).toList();
        assertEquals(1, messages.size(), "Should have only the original message in MongoDB");
    }

    @Test
    void shouldProcessSameSessionDifferentSequenceNumber() throws InterruptedException {
        currentTestSessionGuid = UUID.randomUUID();
        Message firstMessage = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(currentTestSessionGuid)
                .sequenceNumber(1)
                .machineId(1)
                .data(List.of())
                .build();

        messageRepository.save(firstMessage);

        Message secondMessage = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(currentTestSessionGuid)
                .sequenceNumber(2)
                .machineId(1)
                .data(List.of(
                        MessageData.builder()
                                .type(MessageDataType.DISTANCE)
                                .unit("m")
                                .value("200")
                                .build()
                ))
                .build();

        kafkaTemplate.send("inbound_message_queue", secondMessage);

        Thread.sleep(5000);

        assertNotNull(receivedOutboundMessage);
        assertEquals(currentTestSessionGuid, receivedOutboundMessage.sessionGuid());
        assertEquals(2, receivedOutboundMessage.sequenceNumber());

        assertEquals(2, messageService.getMessagesBySessionId(firstMessage.sessionGuid()).size(),
                "Should have two messages in MongoDB for the same session");

    }

    @KafkaListener(topics = "outbound_message_queue", groupId = "test-group")
    public void receiveOutboundMessage(ConsumerRecord<String, Message> record) {
        Message message = record.value();
        System.out.println("%%%% Received outbound message: " + message);
        receivedOutboundMessage = message;
        outboundLatch.countDown();

    }
}