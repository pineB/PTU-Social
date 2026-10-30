package com.ptu.backend.api.dto;

public record ReviewCreateRequest(
        Long activityId,
        Long authorId,
        Integer rating,
        String text
) {
}
