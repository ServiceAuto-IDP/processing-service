package com.serviceauto.processing_service.model;

import java.util.Arrays;

public enum RequestStatus {
    NEW("new"),
    ACCEPTED("accepted"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed");

    private final String value;

    RequestStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RequestStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported request status: " + value));
    }
}
