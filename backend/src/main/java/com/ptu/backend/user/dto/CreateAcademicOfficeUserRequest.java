package com.ptu.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAcademicOfficeUserRequest(
		@NotBlank @Size(max = 200) String fullName,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 12, max = 72) String password) {
}
