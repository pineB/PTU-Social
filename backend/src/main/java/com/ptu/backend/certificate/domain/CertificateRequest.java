package com.ptu.backend.certificate.domain;

import java.time.Instant;
import java.util.UUID;

import com.ptu.backend.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "certificate_requests")
@Getter
@Setter
@NoArgsConstructor
public class CertificateRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false, updatable = false)
	private User student;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private CertificateType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private CertificateStatus status = CertificateStatus.PENDING;

	@Column(name = "rejection_reason")
	private String rejectionReason;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public CertificateRequest(User student, CertificateType type) {
		this.student = student;
		this.type = type;
		this.status = CertificateStatus.PENDING;
	}
}
