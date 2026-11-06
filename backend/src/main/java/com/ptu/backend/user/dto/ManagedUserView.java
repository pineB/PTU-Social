package com.ptu.backend.user.dto;

import java.util.UUID;

import com.ptu.backend.user.domain.User;

public record ManagedUserView(UUID id, String fullName, String email, boolean active) {

	public static ManagedUserView from(User user) {
		return new ManagedUserView(user.getId(), user.getFullName(), user.getEmail(), user.isActive());
	}
}
