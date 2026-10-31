package com.ptu.backend.api.dto;

import com.ptu.backend.domain.ReviewStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
        Long id,
        Long activityId,
        UUID authorId,
        Integer rating,
        String text,
        ReviewStatus status,
        boolean rewardAwarded,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
