package com.ptu.backend.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TeacherBudgetRepository extends JpaRepository<TeacherBudget, Long> {

    boolean existsByTeacherId(UUID teacherId);

    Optional<TeacherBudget> findByTeacherId(UUID teacherId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TeacherBudget budget set budget.spentBudget = budget.spentBudget + :amount " +
            "where budget.teacherId = :teacherId and budget.totalBudget - budget.spentBudget >= :amount")
    int addSpentBudgetIfAvailable(@Param("teacherId") UUID teacherId, @Param("amount") BigDecimal amount);
}
