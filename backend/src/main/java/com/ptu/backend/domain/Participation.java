package com.ptu.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "participations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_participation_activity_student", columnNames = {"activity_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_participation_activity", columnList = "activity_id"),
                @Index(name = "idx_participation_student", columnList = "student_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParticipationStatus status = ParticipationStatus.REGISTERED;

    @Column(name = "points_boa", precision = 5, scale = 2)
    private BigDecimal pointsBoa = BigDecimal.ZERO;

    @Column(name = "points_bua", precision = 5, scale = 2)
    private BigDecimal pointsBua = BigDecimal.ZERO;

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
}
