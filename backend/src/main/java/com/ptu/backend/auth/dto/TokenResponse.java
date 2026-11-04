package com.ptu.backend.auth.dto;

import java.util.UUID;

import com.ptu.backend.user.domain.Role;

public record TokenResponse(
		String accessToken,
		String tokenType,
		long expiresInSeconds,
		UUID userId,
		Role role) {
}
