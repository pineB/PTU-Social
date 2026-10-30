package com.ptu.backend.api.dto;

public record CaseOpenRequest(
        Long caseId,
        Long studentId
) {
}
