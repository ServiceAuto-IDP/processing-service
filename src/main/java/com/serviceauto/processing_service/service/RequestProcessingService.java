package com.serviceauto.processing_service.service;

import com.serviceauto.processing_service.client.IoServiceClient;
import com.serviceauto.processing_service.dto.ProcessingResultResponse;
import com.serviceauto.processing_service.dto.internal.InternalCreateRequestHistoryRequest;
import com.serviceauto.processing_service.dto.internal.InternalServiceRequestResponse;
import com.serviceauto.processing_service.dto.internal.InternalUpdateRequestEstimateRequest;
import com.serviceauto.processing_service.dto.internal.InternalUpdateRequestStatusRequest;
import com.serviceauto.processing_service.messaging.ServiceRequestCreatedMessage;
import com.serviceauto.processing_service.model.RequestCategory;
import com.serviceauto.processing_service.model.RequestStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestProcessingService {

    private static final double ACCEPTED_TO_IN_PROGRESS_RATIO = 0.25d;

    private final IoServiceClient ioServiceClient;
    private final MetricsService metricsService;

    public ProcessingResultResponse processCreatedRequest(ServiceRequestCreatedMessage message) {
        metricsService.incrementMessagesConsumed();
        log.info("Received request_created event for requestId={}, category={}", message.requestId(), message.category());

        InternalServiceRequestResponse request = ioServiceClient.getRequest(message.requestId());
        RequestStatus currentStatus = RequestStatus.fromValue(request.status());
        if (currentStatus != RequestStatus.NEW) {
            log.info("Skipping requestId={} because current status is {}", request.id(), currentStatus.getValue());
            return new ProcessingResultResponse(request.id(), "Request already moved past new");
        }

        RequestCategory category = message.category() != null ? message.category() : request.category();
        Instant now = Instant.now();
        Instant estimate = now.plus(calculateEta(category));

        ioServiceClient.updateEstimate(request.id(), new InternalUpdateRequestEstimateRequest(estimate));
        transition(request, RequestStatus.ACCEPTED, now, "Request accepted automatically by processing-service");

        log.info("RequestId={} accepted with ETA {}", request.id(), estimate);
        return new ProcessingResultResponse(request.id(), "Request accepted for processing");
    }

    public void advanceAcceptedRequests() {
        List<InternalServiceRequestResponse> acceptedRequests = ioServiceClient.getRequestsByStatus(RequestStatus.ACCEPTED.getValue());
        Instant now = Instant.now();

        for (InternalServiceRequestResponse request : acceptedRequests) {
            if (!shouldMoveToInProgress(request, now)) {
                continue;
            }

            transition(request, RequestStatus.IN_PROGRESS, now, "Work started automatically by processing-service");
            log.info("RequestId={} moved to in_progress", request.id());
        }
    }

    public void advanceInProgressRequests() {
        List<InternalServiceRequestResponse> inProgressRequests = ioServiceClient.getRequestsByStatus(RequestStatus.IN_PROGRESS.getValue());
        Instant now = Instant.now();

        for (InternalServiceRequestResponse request : inProgressRequests) {
            if (!shouldMoveToCompleted(request, now)) {
                continue;
            }

            transition(request, RequestStatus.COMPLETED, now, "Request completed automatically by processing-service");
            metricsService.incrementRequestsCompleted();
            metricsService.recordCompletionDuration(Duration.between(request.createdAt(), now));
            log.info("RequestId={} moved to completed", request.id());
        }
    }

    public ProcessingResultResponse retryRequest(Long requestId) {
        InternalServiceRequestResponse request = ioServiceClient.getRequest(requestId);
        if (RequestStatus.fromValue(request.status()) == RequestStatus.COMPLETED) {
            return new ProcessingResultResponse(request.id(), "Request already completed");
        }

        return processCreatedRequest(new ServiceRequestCreatedMessage(
                request.id(),
                request.userId(),
                request.vehicleId(),
                request.category(),
                request.createdAt()
        ));
    }

    Duration calculateEta(RequestCategory category) {
        return category.eta();
    }

    boolean shouldMoveToInProgress(InternalServiceRequestResponse request, Instant now) {
        RequestStatus currentStatus = RequestStatus.fromValue(request.status());
        if (currentStatus != RequestStatus.ACCEPTED) {
            return false;
        }

        Instant acceptedAt = request.updatedAt();
        Duration eta = calculateEta(request.category());
        Duration delay = Duration.ofSeconds(Math.max(1L, Math.round(eta.toSeconds() * ACCEPTED_TO_IN_PROGRESS_RATIO)));
        return !now.isBefore(acceptedAt.plus(delay));
    }

    boolean shouldMoveToCompleted(InternalServiceRequestResponse request, Instant now) {
        RequestStatus currentStatus = RequestStatus.fromValue(request.status());
        if (currentStatus != RequestStatus.IN_PROGRESS || request.estimatedResolutionTime() == null) {
            return false;
        }

        return !now.isBefore(request.estimatedResolutionTime());
    }

    private void transition(
            InternalServiceRequestResponse request,
            RequestStatus nextStatus,
            Instant changedAt,
            String details
    ) {
        RequestStatus currentStatus = RequestStatus.fromValue(request.status());
        if (!isValidTransition(currentStatus, nextStatus)) {
            log.warn(
                    "Ignoring invalid transition for requestId={}, currentStatus={}, nextStatus={}",
                    request.id(),
                    currentStatus.getValue(),
                    nextStatus.getValue()
            );
            return;
        }

        ioServiceClient.updateStatus(request.id(), new InternalUpdateRequestStatusRequest(nextStatus.getValue()));
        ioServiceClient.createHistory(request.id(), new InternalCreateRequestHistoryRequest(
                request.id(),
                currentStatus.getValue(),
                nextStatus.getValue(),
                changedAt,
                details
        ));
    }

    private boolean isValidTransition(RequestStatus currentStatus, RequestStatus nextStatus) {
        return switch (currentStatus) {
            case NEW -> nextStatus == RequestStatus.ACCEPTED;
            case ACCEPTED -> nextStatus == RequestStatus.IN_PROGRESS;
            case IN_PROGRESS -> nextStatus == RequestStatus.COMPLETED;
            case COMPLETED -> false;
        };
    }
}
