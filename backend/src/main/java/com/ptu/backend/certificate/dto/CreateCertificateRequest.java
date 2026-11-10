package com.ptu.backend.certificate.dto;

import com.ptu.backend.certificate.domain.CertificateType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCertificateRequest(
		@NotNull(message = "тип справки обязателен") CertificateType type,
		@Size(max = 120, message = "название не длиннее 120 символов") String customTitle,
		@Size(max = 2000, message = "описание не длиннее 2000 символов") String description,
		@NotNull(message = "способ получения обязателен") String deliveryMethod) {
}
