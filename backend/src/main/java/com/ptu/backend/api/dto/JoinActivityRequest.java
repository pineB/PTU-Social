package com.ptu.backend.api.dto;

public record JoinActivityRequest(
        Long studentId,
        Long activityId,
        Integer count
) {
}
