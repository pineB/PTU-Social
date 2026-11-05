package com.ptu.backend.auth.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ptu.backend.auth.domain.RegistrationRequest;
import com.ptu.backend.auth.domain.RegistrationStatus;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, UUID> {

	boolean existsByEmailIgnoreCaseAndStatusIn(String email, Collection<RegistrationStatus> statuses);

	Page<RegistrationRequest> findByStatus(RegistrationStatus status, Pageable pageable);

	Page<RegistrationRequest> findByStatusAndGroupIdIn(RegistrationStatus status, Collection<UUID> groupIds,
			Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from RegistrationRequest r where r.id = :id")
	Optional<RegistrationRequest> findByIdForUpdate(@Param("id") UUID id);

	java.util.List<RegistrationRequest> findByStatusInAndExpiresAtBefore(Collection<RegistrationStatus> statuses,
			Instant now);
}
