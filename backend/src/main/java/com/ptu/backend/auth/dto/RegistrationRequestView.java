package com.ptu.backend.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.ptu.backend.auth.domain.RegistrationRequest;
import com.ptu.backend.auth.domain.RegistrationStatus;

public record RegistrationRequestView(
		UUID id,
		String fullName,
		String email,
		UUID facultyId,
		UUID groupId,
		RegistrationStatus status,
		Instant createdAt,
		Instant expiresAt) {

	public static RegistrationRequestView from(RegistrationRequest request) {
		return new RegistrationRequestView(
				request.getId(),
				request.getFullName(),
				request.getEmail(),
				request.getFacultyId(),
				request.getGroupId(),
				request.getStatus(),
				request.getCreatedAt(),
				request.getExpiresAt());
	}
}
