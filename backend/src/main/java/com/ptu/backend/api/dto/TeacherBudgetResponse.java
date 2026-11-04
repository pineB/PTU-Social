package com.ptu.backend.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TeacherBudgetResponse(
        UUID teacherId,
        BigDecimal totalBudget,
        BigDecimal spentBudget,
        BigDecimal availableBudget
) {
}
