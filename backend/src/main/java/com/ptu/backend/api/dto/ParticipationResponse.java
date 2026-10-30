package com.ptu.backend.api.dto;

import com.ptu.backend.domain.ParticipationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParticipationResponse(
        Long id,
        Long activityId,
        Long studentId,
        ParticipationStatus status,
        BigDecimal pointsBoa,
        BigDecimal pointsBua,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
