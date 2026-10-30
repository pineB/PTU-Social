package com.ptu.backend.auth.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRegistrationRequest(
		@NotBlank @Size(max = 200) String fullName,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
				message = "должен содержать минимум одну букву и одну цифру") String password,
		@NotNull UUID facultyId,
		@NotNull UUID groupId) {
}
