package com.ptu.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
		@NotBlank @Pattern(regexp = "\\d{6}", message = "код должен состоять из 6 цифр") String code) {
}
