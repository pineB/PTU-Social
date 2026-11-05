package com.ptu.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "teacher_budgets",
        indexes = {@Index(name = "idx_teacher_budget_teacher", columnList = "teacher_id", unique = true)}
)
@Getter
@Setter
@NoArgsConstructor
public class TeacherBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false, unique = true)
    private UUID teacherId;

    @Column(name = "total_budget", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalBudget = BigDecimal.ZERO;

    @Column(name = "spent_budget", precision = 19, scale = 2, nullable = false)
    private BigDecimal spentBudget = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getAvailableBudget() {
        return totalBudget.subtract(spentBudget);
    }
}
