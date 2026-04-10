package com.serviceauto.processing_service.listener;

import com.serviceauto.processing_service.messaging.ServiceRequestCreatedMessage;
import com.serviceauto.processing_service.service.MetricsService;
import com.serviceauto.processing_service.service.RequestProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRequestCreatedListener {

    private final RequestProcessingService requestProcessingService;
    private final MetricsService metricsService;

    @RabbitListener(queues = "${app.messaging.request-created-queue}")
    public void onMessage(ServiceRequestCreatedMessage message) {
        try {
            requestProcessingService.processCreatedRequest(message);
        } catch (RuntimeException exception) {
            metricsService.incrementErrors();
            log.error("Failed to process request_created event for requestId={}", message.requestId(), exception);
            throw exception;
        }
    }
}
