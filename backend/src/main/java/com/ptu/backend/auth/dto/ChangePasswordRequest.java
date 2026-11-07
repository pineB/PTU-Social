package com.ptu.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotBlank String currentPassword,
		@NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
				message = "должен содержать минимум одну букву и одну цифру") String newPassword) {
}
