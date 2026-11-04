package com.ptu.backend.user.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
public class StudentProfile {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "group_id", nullable = false)
	private UUID groupId;

	@Column(name = "is_group_leader", nullable = false)
	private boolean groupLeader;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	@PreUpdate
	void touch() {
		updatedAt = Instant.now();
	}
}
