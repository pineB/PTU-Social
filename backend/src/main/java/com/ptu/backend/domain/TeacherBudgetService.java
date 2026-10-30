package com.ptu.backend.domain;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public BigDecimal calculateAwardAmount(BigDecimal baseAmount, int percent) {
        if (baseAmount == null) {
            throw new IllegalArgumentException("Base amount is required");
        }
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Percent must be between 0 and 100");
        }
        return baseAmount.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalAward(BigDecimal boaBase, BigDecimal buaBase, int boaPercent, int buaPercent) {
        BigDecimal boa = calculateAwardAmount(boaBase, boaPercent);
        BigDecimal bua = calculateAwardAmount(buaBase, buaPercent);
        return boa.add(bua);
    }
}
