package com.serviceauto.processing_service.dto.internal;

import java.time.Instant;

public record InternalCreateRequestHistoryRequest(
        Long requestId,
        String oldStatus,
        String newStatus,
        Instant changedAt,
        String details
) {
}
