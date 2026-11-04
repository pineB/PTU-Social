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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "registration_requests")
@Getter
@Setter
@NoArgsConstructor
public class RegistrationRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, columnDefinition = "citext")
	private String email;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(name = "faculty_id", nullable = false)
	private UUID facultyId;

	@Column(name = "group_id", nullable = false)
	private UUID groupId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private RegistrationStatus status = RegistrationStatus.PENDING_EMAIL;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
