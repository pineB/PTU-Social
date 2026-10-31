package com.ptu.backend.certificate.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ptu.backend.certificate.domain.CertificateRequest;
import com.ptu.backend.certificate.domain.CertificateStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, UUID> {

	@EntityGraph(attributePaths = "student")
	List<CertificateRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

	@EntityGraph(attributePaths = "student")
	Page<CertificateRequest> findAllByOrderByCreatedAtAsc(Pageable pageable);

	@EntityGraph(attributePaths = "student")
	Page<CertificateRequest> findByStatusOrderByCreatedAtAsc(CertificateStatus status, Pageable pageable);

	@Query("select request from CertificateRequest request join fetch request.student where request.id = :id")
	Optional<CertificateRequest> findWithStudentById(@Param("id") UUID id);
}
