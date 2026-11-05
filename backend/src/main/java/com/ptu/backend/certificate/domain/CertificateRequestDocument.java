package com.ptu.backend.certificate.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "certificate_request_documents")
@Getter
@Setter
@NoArgsConstructor
public class CertificateRequestDocument {

	@Id
	@Column(name = "request_id")
	private UUID requestId;

	@Column(name = "document_name", nullable = false, length = 255)
	private String documentName;

	@Column(name = "document_data", nullable = false, columnDefinition = "bytea")
	private byte[] documentData;

	public CertificateRequestDocument(UUID requestId, String documentName, byte[] documentData) {
		this.requestId = requestId;
		this.documentName = documentName;
		this.documentData = documentData;
	}
}
