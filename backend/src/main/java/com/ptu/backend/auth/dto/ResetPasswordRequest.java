package com.ptu.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "должен состоять из 6 цифр") String code,
		@NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
				message = "должен содержать минимум одну букву и одну цифру") String newPassword) {
}
