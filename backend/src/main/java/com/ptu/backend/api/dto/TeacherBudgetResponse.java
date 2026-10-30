package com.ptu.backend.api.dto;

import java.math.BigDecimal;

public record TeacherBudgetResponse(
        Long teacherId,
        BigDecimal totalBudget,
        BigDecimal spentBudget,
        BigDecimal availableBudget
) {
}
