package com.ptu.backend.certificate.dto;

import com.ptu.backend.certificate.domain.CertificateStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCertificateStatusRequest(
		@NotNull(message = "статус обязателен") CertificateStatus status,
		@Size(max = 1000, message = "причина отклонения не длиннее 1000 символов") String rejectionReason) {
}
