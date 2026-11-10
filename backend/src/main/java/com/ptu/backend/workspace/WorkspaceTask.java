package com.ptu.backend.workspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workspace_tasks")
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceTask {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "teacher_id", nullable = false)
	private UUID teacherId;

	@Column(name = "group_id", nullable = false)
	private UUID groupId;

	@Column(name = "discipline_id", nullable = false)
	private UUID disciplineId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(name = "due_at", nullable = false)
	private Instant dueAt;

	@Column(name = "max_points", nullable = false)
	private int maxPoints;

	@Column(name = "allow_late_submission", nullable = false)
	private boolean allowLateSubmission;

	@Column(name = "attachment_name", length = 255)
	private String attachmentName;

	@Column(name = "attachment_content_type", length = 120)
	private String attachmentContentType;

	@Column(name = "attachment_data", columnDefinition = "bytea")
	private byte[] attachmentData;

	@Column(nullable = false)
	private boolean archived;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}
}
