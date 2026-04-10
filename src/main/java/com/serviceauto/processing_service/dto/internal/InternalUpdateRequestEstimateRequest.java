package com.serviceauto.processing_service.dto.internal;

import java.time.Instant;

public record InternalUpdateRequestEstimateRequest(Instant estimatedResolutionTime) {
}
