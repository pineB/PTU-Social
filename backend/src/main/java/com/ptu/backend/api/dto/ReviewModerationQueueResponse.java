package com.ptu.backend.api.dto;

import java.util.List;

public record ReviewModerationQueueResponse(
        List<ReviewResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
