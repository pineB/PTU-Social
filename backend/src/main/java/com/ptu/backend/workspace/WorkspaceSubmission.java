package com.ptu.backend.workspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workspace_submissions")
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceSubmission {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "task_id", nullable = false)
	private UUID taskId;

	@Column(name = "student_id", nullable = false)
	private UUID studentId;

	@Column(name = "submitted_at", nullable = false)
	private Instant submittedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private WorkspaceSubmissionStatus status;

	@Column(columnDefinition = "TEXT")
	private String content;

	@Column(name = "attachment_name", length = 255)
	private String attachmentName;

	@Column(name = "attachment_content_type", length = 120)
	private String attachmentContentType;

	@Column(name = "attachment_data", columnDefinition = "bytea")
	private byte[] attachmentData;

	@Column(name = "teacher_feedback", columnDefinition = "TEXT")
	private String teacherFeedback;

	private Integer grade;

	@Column(name = "awarded_rating")
	private Integer awardedRating;

	@Column(name = "revision_count", nullable = false)
	private int revisionCount;

	@PrePersist
	void touch() {
		if (submittedAt == null) submittedAt = Instant.now();
	}
}
