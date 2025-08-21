package com.johndeere.challenge.model.dto;

import com.johndeere.challenge.model.MessageData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record MessageDto(

        @NotNull(message = "Session UUID cannot be null")
        UUID sessionGuid,

        @Positive(message = "Sequence number must be positive")
        int sequenceNumber,

        @Positive(message = "Machine ID must be positive")
        int machineId,

        @Valid
        @NotNull(message = "Data cannot be null")
        List<MessageData> data
) {
}