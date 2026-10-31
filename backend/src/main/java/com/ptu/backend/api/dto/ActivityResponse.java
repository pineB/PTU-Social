package com.ptu.backend.api.dto;

import com.ptu.backend.domain.ActivityType;
import com.ptu.backend.domain.ParticipationMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityResponse(
        Long id,
        String title,
        String description,
        ActivityType type,
        ParticipationMode participationMode,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer maxParticipants,
        Integer minParticipants,
        Integer requiredActivationPercent,
        BigDecimal rewardBoa,
        BigDecimal rewardBua,
        UUID teacherId,
        Boolean active,
        LocalDateTime createdAt
) {
}
