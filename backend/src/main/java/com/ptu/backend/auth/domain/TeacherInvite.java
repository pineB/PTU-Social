package com.ptu.backend.auth.domain;

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
@Table(name = "teacher_invites")
@Getter
@Setter
@NoArgsConstructor
public class TeacherInvite {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, columnDefinition = "citext")
	private String email;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(name = "department_id", nullable = false)
	private UUID departmentId;

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private TeacherInviteStatus status = TeacherInviteStatus.PENDING;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}
}
