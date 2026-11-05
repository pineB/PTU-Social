package com.ptu.backend.auth.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTeacherInviteRequest(
		@NotBlank @Size(max = 200) String fullName,
		@NotBlank @Email @Size(max = 255) String email,
		@NotNull UUID departmentId,
		@NotEmpty Set<UUID> disciplineIds,
		@NotEmpty Set<UUID> groupIds) {
}
