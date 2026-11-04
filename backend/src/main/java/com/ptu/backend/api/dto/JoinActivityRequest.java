package com.ptu.backend.api.dto;

import java.util.UUID;

public record JoinActivityRequest(
        UUID studentId,
        Long activityId,
        Integer count
) {
}
