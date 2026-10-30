package com.ptu.backend.web.dto;

import com.ptu.backend.domain.SubmissionStatus;

public record ReviewSubmissionRequest(
    SubmissionStatus status,
    String feedback,
    Integer baseAcademicRating
) {
}
