package com.johndeere.challenge.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.UUID;


@Builder
public record Message(
        @Id
        @NotNull(message = "Message ID cannot be null")
        UUID id,

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