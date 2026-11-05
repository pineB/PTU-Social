package com.ptu.backend.certificate.repo;

import java.util.UUID;
import java.util.Optional;

import com.ptu.backend.certificate.domain.CertificateRequestDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificateRequestDocumentRepository extends JpaRepository<CertificateRequestDocument, UUID> {

	@Query("select document.documentName from CertificateRequestDocument document where document.requestId = :requestId")
	Optional<String> findDocumentNameByRequestId(@Param("requestId") UUID requestId);
}
