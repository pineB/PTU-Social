package com.ptu.backend.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TeacherBudgetService {

    private final TeacherBudgetRepository teacherBudgetRepository;

    public TeacherBudgetService(TeacherBudgetRepository teacherBudgetRepository) {
        this.teacherBudgetRepository = teacherBudgetRepository;
    }

    @Transactional
    public TeacherBudget awardPoints(UUID teacherId, BigDecimal awardAmount) {
        if (teacherId == null) {
            throw new IllegalArgumentException("Teacher id must be provided");
        }
        if (awardAmount == null || awardAmount.signum() < 0) {
            throw new IllegalArgumentException("Award amount must be non-negative");
        }

        int updatedRows = teacherBudgetRepository.addSpentBudgetIfAvailable(teacherId, awardAmount);
        if (updatedRows == 1) {
            return teacherBudgetRepository.findByTeacherId(teacherId)
                    .orElseThrow(() -> new IllegalStateException("Teacher budget disappeared during award"));
        }
        if (!teacherBudgetRepository.existsByTeacherId(teacherId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher budget not found");
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher budget exceeded");
    }

    public BigDecimal calculateAwardAmount(BigDecimal baseAmount, int percent) {
        if (baseAmount == null || baseAmount.signum() < 0) {
            throw new IllegalArgumentException("Base amount must be non-negative");
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
