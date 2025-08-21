package com.johndeere.challenge.model;

import lombok.Builder;

@Builder
public record MessageData(
        MessageDataType type,
        String unit,
        String value
) {
}