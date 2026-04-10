package com.serviceauto.processing_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.serviceauto.processing_service.client.IoServiceClient;
import com.serviceauto.processing_service.dto.internal.InternalServiceRequestResponse;
import com.serviceauto.processing_service.messaging.ServiceRequestCreatedMessage;
import com.serviceauto.processing_service.model.RequestCategory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestProcessingServiceTest {

    @Mock
    private IoServiceClient ioServiceClient;

    @Mock
    private MetricsService metricsService;

    @Captor
    private ArgumentCaptor<com.serviceauto.processing_service.dto.internal.InternalUpdateRequestEstimateRequest> estimateCaptor;

    @Captor
    private ArgumentCaptor<com.serviceauto.processing_service.dto.internal.InternalUpdateRequestStatusRequest> statusCaptor;

    @Captor
    private ArgumentCaptor<com.serviceauto.processing_service.dto.internal.InternalCreateRequestHistoryRequest> historyCaptor;

    private RequestProcessingService requestProcessingService;

    @BeforeEach
    void setUp() {
        requestProcessingService = new RequestProcessingService(ioServiceClient, metricsService);
    }

    @Test
    void processCreatedRequestAcceptsNewRequestAndStoresEstimate() {
        Instant createdAt = Instant.parse("2026-04-11T10:00:00Z");
        InternalServiceRequestResponse request = request(10L, "new", RequestCategory.MECHANICAL, null, createdAt, createdAt);
        when(ioServiceClient.getRequest(10L)).thenReturn(request);

        requestProcessingService.processCreatedRequest(new ServiceRequestCreatedMessage(
                10L,
                7L,
                4L,
                RequestCategory.MECHANICAL,
                createdAt
        ));

        verify(metricsService).incrementMessagesConsumed();
        verify(ioServiceClient).updateEstimate(any(), estimateCaptor.capture());
        verify(ioServiceClient).updateStatus(any(), statusCaptor.capture());
        verify(ioServiceClient).createHistory(any(), historyCaptor.capture());
        assertEquals("accepted", statusCaptor.getValue().status());
        assertEquals("new", historyCaptor.getValue().oldStatus());
        assertEquals("accepted", historyCaptor.getValue().newStatus());
        assertTrue(estimateCaptor.getValue().estimatedResolutionTime().isAfter(Instant.now().minusSeconds(5)));
    }

    @Test
    void processCreatedRequestSkipsAlreadyProcessedRequest() {
        Instant now = Instant.parse("2026-04-11T10:00:00Z");
        when(ioServiceClient.getRequest(10L)).thenReturn(request(
                10L,
                "accepted",
                RequestCategory.ELECTRICAL,
                now.plusSeconds(30),
                now,
                now
        ));

        requestProcessingService.processCreatedRequest(new ServiceRequestCreatedMessage(
                10L,
                7L,
                4L,
                RequestCategory.ELECTRICAL,
                now
        ));

        verify(metricsService).incrementMessagesConsumed();
        verify(ioServiceClient, never()).updateEstimate(any(), any());
        verify(ioServiceClient, never()).updateStatus(any(), any());
        verify(ioServiceClient, never()).createHistory(any(), any());
    }

    @Test
    void advanceAcceptedRequestsMovesEligibleRequestToInProgress() {
        Instant acceptedAt = Instant.now().minusSeconds(15);
        when(ioServiceClient.getRequestsByStatus("accepted")).thenReturn(List.of(request(
                10L,
                "accepted",
                RequestCategory.MECHANICAL,
                acceptedAt.plusSeconds(30),
                acceptedAt.minusSeconds(30),
                acceptedAt
        )));

        requestProcessingService.advanceAcceptedRequests();

        verify(ioServiceClient).updateStatus(any(), statusCaptor.capture());
        verify(ioServiceClient).createHistory(any(), historyCaptor.capture());
        assertEquals("in_progress", statusCaptor.getValue().status());
        assertEquals("accepted", historyCaptor.getValue().oldStatus());
        assertEquals("in_progress", historyCaptor.getValue().newStatus());
    }

    @Test
    void advanceInProgressRequestsCompletesEligibleRequest() {
        Instant createdAt = Instant.now().minusSeconds(50);
        when(ioServiceClient.getRequestsByStatus("in_progress")).thenReturn(List.of(request(
                10L,
                "in_progress",
                RequestCategory.TIRE_SERVICE,
                Instant.now().minusSeconds(1),
                createdAt,
                Instant.now().minusSeconds(10)
        )));

        requestProcessingService.advanceInProgressRequests();

        verify(ioServiceClient).updateStatus(any(), statusCaptor.capture());
        verify(ioServiceClient).createHistory(any(), historyCaptor.capture());
        verify(metricsService).incrementRequestsCompleted();
        assertEquals("completed", statusCaptor.getValue().status());
        assertEquals("in_progress", historyCaptor.getValue().oldStatus());
        assertEquals("completed", historyCaptor.getValue().newStatus());
    }

    @Test
    void shouldMoveToInProgressWaitsForQuarterEta() {
        Instant acceptedAt = Instant.parse("2026-04-11T10:00:00Z");
        InternalServiceRequestResponse request = request(
                10L,
                "accepted",
                RequestCategory.PAINTING,
                acceptedAt.plusSeconds(60),
                acceptedAt.minusSeconds(20),
                acceptedAt
        );

        assertFalse(requestProcessingService.shouldMoveToInProgress(request, acceptedAt.plusSeconds(10)));
        assertTrue(requestProcessingService.shouldMoveToInProgress(request, acceptedAt.plusSeconds(15)));
    }

    private InternalServiceRequestResponse request(
            Long id,
            String status,
            RequestCategory category,
            Instant estimatedResolutionTime,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new InternalServiceRequestResponse(
                id,
                7L,
                4L,
                category,
                "Noise in the front suspension",
                status,
                estimatedResolutionTime,
                createdAt,
                updatedAt
        );
    }
}
