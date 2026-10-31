package com.ptu.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "semester_academic_ratings")
@Getter
@Setter
@NoArgsConstructor
public class SemesterAcademicRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "total_rating", nullable = false)
    private Integer totalRating = 0;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt = LocalDateTime.now();

    @Column(name = "semester_label", nullable = false)
    private String semesterLabel;
}
