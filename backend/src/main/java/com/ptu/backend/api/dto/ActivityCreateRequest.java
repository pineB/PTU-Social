package com.ptu.backend.api.dto;

import com.ptu.backend.domain.ActivityType;
import com.ptu.backend.domain.ParticipationMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActivityCreateRequest(
        String title,
        String description,
        ActivityType type,
        ParticipationMode participationMode,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer maxParticipants,
        Integer minParticipants,
        Integer requiredActivationPercent,
        Long teacherId,
        BigDecimal rewardBoa,
        BigDecimal rewardBua
) {
}
