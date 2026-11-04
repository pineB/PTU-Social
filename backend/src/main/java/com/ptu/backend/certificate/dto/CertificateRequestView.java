package com.ptu.backend.certificate.dto;

import java.time.Instant;
import java.util.UUID;

import com.ptu.backend.certificate.domain.CertificateRequest;
import com.ptu.backend.certificate.domain.CertificateStatus;
import com.ptu.backend.certificate.domain.CertificateType;

public record CertificateRequestView(
		UUID id,
		UUID studentId,
		String studentName,
		CertificateType type,
		CertificateStatus status,
		String rejectionReason,
		Instant createdAt,
		Instant updatedAt) {

	public static CertificateRequestView from(CertificateRequest request) {
		return new CertificateRequestView(
				request.getId(),
				request.getStudent().getId(),
				request.getStudent().getFullName(),
				request.getType(),
				request.getStatus(),
				request.getRejectionReason(),
				request.getCreatedAt(),
				request.getUpdatedAt());
	}
}
