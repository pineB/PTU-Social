package com.ptu.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "legacy_submissions")
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "attachment_url", length = 2048)
    private String attachmentUrl;

    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    @Column(name = "revision_count", nullable = false)
    private Integer revisionCount = 0;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "awarded_rating")
    private Integer awardedRating;

    @Column(nullable = false)
    private Boolean archived = false;
}
