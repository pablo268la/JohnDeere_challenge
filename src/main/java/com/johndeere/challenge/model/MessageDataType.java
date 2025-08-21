package com.johndeere.challenge.model;

public enum MessageDataType {
    DISTANCE("distance"),
    WORKED_SURFACE("workedSurface"),
    ;

    private final String jsonValue;

    MessageDataType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    public static MessageDataType fromString(String value) {
        for (MessageDataType type : values()) {
            if (type.jsonValue.equalsIgnoreCase(value) || type.name().replace("_", "").equalsIgnoreCase(value.replace("_", ""))) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de dato no soportado: " + value);
    }

    @Override
    public String toString() {
        return jsonValue;
    }
}