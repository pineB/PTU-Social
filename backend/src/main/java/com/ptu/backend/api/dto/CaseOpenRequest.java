package com.ptu.backend.api.dto;

import java.util.UUID;

public record CaseOpenRequest(
        Long caseId,
        UUID studentId
) {
}
