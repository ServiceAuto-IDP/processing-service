package com.serviceauto.processing_service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Duration;
import java.util.Arrays;

public enum RequestCategory {
    MECHANICAL("mechanical", Duration.ofSeconds(40)),
    ELECTRICAL("electrical", Duration.ofSeconds(30)),
    PAINTING("painting", Duration.ofSeconds(60)),
    TIRE_SERVICE("tire_service", Duration.ofSeconds(20));

    private final String value;
    private final Duration eta;

    RequestCategory(String value, Duration eta) {
        this.value = value;
        this.eta = eta;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public Duration eta() {
        return eta;
    }

    @JsonCreator
    public static RequestCategory fromValue(String value) {
        return Arrays.stream(values())
                .filter(category -> category.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid category. Accepted values: mechanical, electrical, painting, tire_service"
                ));
    }
}
