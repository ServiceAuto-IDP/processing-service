package com.serviceauto.processing_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter consumedMessagesCounter;
    private final Counter processedRequestsCounter;
    private final Counter processingErrorsCounter;
    private final Timer completedProcessingTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.consumedMessagesCounter = Counter.builder("processing_service_messages_consumed_total")
                .description("Number of service request created messages consumed")
                .register(meterRegistry);
        this.processedRequestsCounter = Counter.builder("processing_service_requests_completed_total")
                .description("Number of service requests completed by processing-service")
                .register(meterRegistry);
        this.processingErrorsCounter = Counter.builder("processing_service_errors_total")
                .description("Number of processing-service errors")
                .register(meterRegistry);
        this.completedProcessingTimer = Timer.builder("processing_service_completion_duration")
                .description("Time between request creation and completion")
                .register(meterRegistry);
    }

    public void incrementMessagesConsumed() {
        consumedMessagesCounter.increment();
    }

    public void incrementRequestsCompleted() {
        processedRequestsCounter.increment();
    }

    public void incrementErrors() {
        processingErrorsCounter.increment();
    }

    public void recordCompletionDuration(Duration duration) {
        completedProcessingTimer.record(duration);
    }
}
