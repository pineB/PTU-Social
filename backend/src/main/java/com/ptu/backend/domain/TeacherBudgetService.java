package com.ptu.backend.domain;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public class TeacherBudgetService {

    @Transactional
    public TeacherBudget awardPoints(TeacherBudget budget, BigDecimal awardAmount) {
        if (budget == null) {
            throw new IllegalArgumentException("Teacher budget must be provided");
        }
        if (awardAmount == null || awardAmount.signum() < 0) {
            throw new IllegalArgumentException("Award amount must be non-negative");
        }

        BigDecimal availableBudget = budget.getAvailableBudget();
        if (awardAmount.compareTo(availableBudget) > 0) {
            throw new IllegalArgumentException("Teacher budget exceeded");
        }

        budget.setSpentBudget(budget.getSpentBudget().add(awardAmount));
        return budget;
    }
}
