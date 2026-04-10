package com.serviceauto.processing_service.scheduler;

import com.serviceauto.processing_service.service.MetricsService;
import com.serviceauto.processing_service.service.RequestProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RequestProcessingScheduler {

    private final RequestProcessingService requestProcessingService;
    private final MetricsService metricsService;

    @Scheduled(fixedDelayString = "${app.scheduler.poll-delay:10s}")
    public void advanceAcceptedRequests() {
        try {
            requestProcessingService.advanceAcceptedRequests();
        } catch (RuntimeException exception) {
            metricsService.incrementErrors();
            log.error("Failed to advance accepted requests", exception);
        }
    }

    @Scheduled(fixedDelayString = "${app.scheduler.poll-delay:10s}", initialDelayString = "${app.scheduler.poll-delay:10s}")
    public void advanceInProgressRequests() {
        try {
            requestProcessingService.advanceInProgressRequests();
        } catch (RuntimeException exception) {
            metricsService.incrementErrors();
            log.error("Failed to advance in-progress requests", exception);
        }
    }
}
