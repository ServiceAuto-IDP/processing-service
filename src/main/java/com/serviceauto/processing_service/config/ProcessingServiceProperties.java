package com.serviceauto.processing_service.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record ProcessingServiceProperties(
        IoService ioService,
        Messaging messaging,
        Scheduler scheduler
) {

    public record IoService(String baseUrl) {
    }

    public record Messaging(
            String requestCreatedExchange,
            String requestCreatedQueue,
            String requestCreatedRoutingKey
    ) {
    }

    public record Scheduler(Duration pollDelay) {
    }
}
