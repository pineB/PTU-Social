package com.ptu.backend.auth.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptu.backend.auth.domain.TeacherInvite;
import com.ptu.backend.auth.domain.TeacherInviteStatus;

public interface TeacherInviteRepository extends JpaRepository<TeacherInvite, UUID> {

	Optional<TeacherInvite> findByTokenHash(String tokenHash);

	boolean existsByEmailIgnoreCaseAndStatus(String email, TeacherInviteStatus status);

	List<TeacherInvite> findByStatusAndExpiresAtBefore(TeacherInviteStatus status, Instant now);
}
