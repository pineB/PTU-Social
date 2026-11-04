package com.ptu.backend.api.dto;

import com.ptu.backend.domain.ParticipationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ParticipationResponse(
        Long id,
        Long activityId,
        UUID studentId,
        ParticipationStatus status,
        BigDecimal pointsBoa,
        BigDecimal pointsBua,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
