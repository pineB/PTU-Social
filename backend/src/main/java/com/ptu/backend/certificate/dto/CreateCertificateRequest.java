package com.ptu.backend.certificate.dto;

import com.ptu.backend.certificate.domain.CertificateType;

import jakarta.validation.constraints.NotNull;

public record CreateCertificateRequest(@NotNull(message = "тип справки обязателен") CertificateType type) {
}
