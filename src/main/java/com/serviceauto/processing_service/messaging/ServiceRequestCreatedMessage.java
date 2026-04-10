package com.serviceauto.processing_service.messaging;

import com.serviceauto.processing_service.model.RequestCategory;
import java.time.Instant;

public record ServiceRequestCreatedMessage(
        Long requestId,
        Long userId,
        Long vehicleId,
        RequestCategory category,
        Instant createdAt
) {
}
