package com.serviceauto.processing_service.client;

import com.serviceauto.processing_service.dto.internal.InternalCreateRequestHistoryRequest;
import com.serviceauto.processing_service.dto.internal.InternalServiceRequestResponse;
import com.serviceauto.processing_service.dto.internal.InternalUpdateRequestEstimateRequest;
import com.serviceauto.processing_service.dto.internal.InternalUpdateRequestStatusRequest;
import com.serviceauto.processing_service.exception.RequestNotFoundException;
import com.serviceauto.processing_service.exception.UpstreamServiceException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class IoServiceClient {

    private final RestClient ioServiceRestClient;

    public InternalServiceRequestResponse getRequest(Long requestId) {
        try {
            return ioServiceRestClient.get()
                    .uri("/internal/requests/{requestId}", requestId)
                    .retrieve()
                    .body(InternalServiceRequestResponse.class);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new RequestNotFoundException(requestId);
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("Failed to fetch service request from io-service");
        }
    }

    public List<InternalServiceRequestResponse> getRequestsByStatus(String status) {
        try {
            InternalServiceRequestResponse[] response = ioServiceRestClient.get()
                    .uri("/internal/requests/status/{status}", status)
                    .retrieve()
                    .body(InternalServiceRequestResponse[].class);
            return response == null ? List.of() : List.of(response);
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("Failed to fetch service requests by status from io-service");
        }
    }

    public void updateEstimate(Long requestId, InternalUpdateRequestEstimateRequest request) {
        try {
            ioServiceRestClient.put()
                    .uri("/internal/requests/{requestId}/estimate", requestId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new RequestNotFoundException(requestId);
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("Failed to update request estimate in io-service");
        }
    }

    public void updateStatus(Long requestId, InternalUpdateRequestStatusRequest request) {
        try {
            ioServiceRestClient.put()
                    .uri("/internal/requests/{requestId}/status", requestId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new RequestNotFoundException(requestId);
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("Failed to update request status in io-service");
        }
    }

    public void createHistory(Long requestId, InternalCreateRequestHistoryRequest request) {
        try {
            ioServiceRestClient.post()
                    .uri("/internal/requests/{requestId}/history", requestId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new RequestNotFoundException(requestId);
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("Failed to create request history in io-service");
        }
    }
}
