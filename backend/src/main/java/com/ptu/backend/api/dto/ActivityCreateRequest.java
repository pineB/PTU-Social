package com.ptu.backend.api.dto;

import com.ptu.backend.domain.ActivityType;
import com.ptu.backend.domain.ParticipationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityCreateRequest(
        @NotBlank String title,
        String description,
        @NotNull ActivityType type,
        @NotNull ParticipationMode participationMode,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        @NotNull @Positive Integer maxParticipants,
        @NotNull @PositiveOrZero Integer minParticipants,
        @NotNull @PositiveOrZero Integer requiredActivationPercent,
        @NotNull UUID teacherId,
        @NotNull BigDecimal rewardBoa,
        @NotNull BigDecimal rewardBua
) {
}
