package com.johndeere.challenge.service;

import com.johndeere.challenge.config.MachineConfig;
import com.johndeere.challenge.model.Message;
import com.johndeere.challenge.model.MessageData;
import com.johndeere.challenge.model.MessageDataType;
import com.johndeere.challenge.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.client.api.PetApi;
import org.openapitools.client.model.Pet;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MachineConfig machineConfig;

    @Mock
    private PetApi petApi;

    @InjectMocks
    private MessageService messageService;

    private Message testMessage;
    private UUID testSessionGuid;

    @BeforeEach
    void setUp() {
        testSessionGuid = UUID.randomUUID();
        testMessage = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(testSessionGuid)
                .sequenceNumber(1)
                .machineId(1)
                .data(List.of(
                        MessageData.builder()
                                .type(MessageDataType.DISTANCE)
                                .unit("m")
                                .value("100")
                                .build()
                ))
                .build();
    }

    @Test
    void consumeMessage_ValidAuthorizedMessage_ReturnsTrue() {

        when(machineConfig.getWhitelist()).thenReturn(List.of(1, 2, 3));
        when(petApi.getPetById(1L)).thenReturn(new Pet());
        when(messageRepository.findBySessionGuid(any())).thenReturn(List.of());

        boolean result = messageService.consumeMessage(testMessage);

        assertTrue(result);
        verify(messageRepository).save(testMessage);
    }

    @Test
    void consumeMessage_UnauthorizedMachine_ReturnsFalse() {

        when(machineConfig.getWhitelist()).thenReturn(List.of(2, 3, 4)); // Machine 1 not in whitelist
        when(petApi.getPetById(1L)).thenReturn(new Pet());
        when(messageRepository.findBySessionGuid(any())).thenReturn(List.of());


        boolean result = messageService.consumeMessage(testMessage);


        assertFalse(result);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void consumeMessage_DuplicateSequenceNumber_ReturnsFalse() {

        Message existingMessage = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(testSessionGuid)
                .sequenceNumber(1) // Same sequence number
                .machineId(1)
                .data(List.of())
                .build();

        when(machineConfig.getWhitelist()).thenReturn(List.of(1, 2, 3));
        when(petApi.getPetById(1L)).thenReturn(new Pet());
        when(messageRepository.findBySessionGuid(any()))
                .thenReturn(List.of(existingMessage));


        boolean result = messageService.consumeMessage(testMessage);


        assertFalse(result);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void consumeMessage_SameSessionDifferentSequence_ReturnsTrue() {

        Message existingMessage = Message.builder()
                .id(UUID.randomUUID())
                .sessionGuid(testSessionGuid)
                .sequenceNumber(2)
                .machineId(1)
                .data(List.of())
                .build();

        when(machineConfig.getWhitelist()).thenReturn(List.of(1, 2, 3));
        when(petApi.getPetById(1L)).thenReturn(new Pet());
        when(messageRepository.findBySessionGuid(any()))
                .thenReturn(List.of(existingMessage));


        boolean result = messageService.consumeMessage(testMessage);


        assertTrue(result);
        verify(messageRepository).save(testMessage);
    }

    @Test
    void consumeMessage_NullMessage_ReturnsFalse() {

        boolean result = messageService.consumeMessage(null);


        assertFalse(result);
        verify(messageRepository, never()).save(any());
    }


    @Test
    void consumeMessage_ExternalApiFailure_ReturnsFalse() {

        when(petApi.getPetById(1L)).thenThrow(new RuntimeException("API failure"));


        boolean result = messageService.consumeMessage(testMessage);


        assertFalse(result);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void saveMessage_DatabaseFailure_ThrowsException() {

        when(messageRepository.save(any())).thenThrow(new RuntimeException("Database failure"));

        assertThrows(RuntimeException.class, () -> messageService.saveMessage(testMessage));
    }


}