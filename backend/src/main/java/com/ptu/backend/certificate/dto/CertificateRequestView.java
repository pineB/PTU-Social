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
		String customTitle,
		String description,
		String deliveryMethod,
		CertificateStatus status,
		String rejectionReason,
		String documentName,
		boolean documentAvailable,
		Instant createdAt,
		Instant updatedAt) {

	public static CertificateRequestView from(CertificateRequest request, String documentName) {
		return new CertificateRequestView(
				request.getId(),
				request.getStudent().getId(),
				request.getStudent().getFullName(),
				request.getType(),
				request.getCustomTitle(),
				request.getDescription(),
				request.getDeliveryMethod(),
				request.getStatus(),
				request.getRejectionReason(),
				documentName,
				documentName != null,
				request.getCreatedAt(),
				request.getUpdatedAt());
	}
}
