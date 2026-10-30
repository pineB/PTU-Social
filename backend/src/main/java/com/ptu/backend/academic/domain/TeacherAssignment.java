package com.ptu.backend.academic.domain;

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
@Table(name = "teacher_assignments")
@Getter
@Setter
@NoArgsConstructor
public class TeacherAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "teacher_id", nullable = false)
	private UUID teacherId;

	@Column(name = "discipline_id", nullable = false)
	private UUID disciplineId;

	@Column(name = "group_id", nullable = false)
	private UUID groupId;

	@Column(name = "semester_id", nullable = false)
	private UUID semesterId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private AssignmentStatus status = AssignmentStatus.ACTIVE;

	@Column(name = "assigned_at", nullable = false)
	private Instant assignedAt;

	@PrePersist
	void prePersist() {
		if (assignedAt == null) {
			assignedAt = Instant.now();
		}
	}

	public TeacherAssignment(UUID teacherId, UUID disciplineId, UUID groupId, UUID semesterId) {
		this.teacherId = teacherId;
		this.disciplineId = disciplineId;
		this.groupId = groupId;
		this.semesterId = semesterId;
	}
}
