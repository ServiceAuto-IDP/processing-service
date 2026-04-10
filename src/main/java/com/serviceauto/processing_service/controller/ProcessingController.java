package com.serviceauto.processing_service.controller;

import com.serviceauto.processing_service.dto.ProcessingResultResponse;
import com.serviceauto.processing_service.service.RequestProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/processing")
@RequiredArgsConstructor
public class ProcessingController {

    private final RequestProcessingService requestProcessingService;

    @PostMapping("/start/{requestId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProcessingResultResponse start(@PathVariable Long requestId) {
        return requestProcessingService.retryRequest(requestId);
    }

    @PostMapping("/retry/{requestId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProcessingResultResponse retry(@PathVariable Long requestId) {
        return requestProcessingService.retryRequest(requestId);
    }
}
