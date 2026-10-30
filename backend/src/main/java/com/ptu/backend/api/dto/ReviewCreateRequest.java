package com.ptu.backend.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotNull Long activityId,
        @NotNull Long authorId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(min = 20, max = 1000) String text
) {
}
