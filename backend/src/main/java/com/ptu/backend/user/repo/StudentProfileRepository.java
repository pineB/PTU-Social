package com.ptu.backend.user.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ptu.backend.user.domain.StudentProfile;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

	Optional<StudentProfile> findByUserId(UUID userId);

	List<StudentProfile> findAllByGroupId(UUID groupId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select profile from StudentProfile profile where profile.userId = :userId")
	Optional<StudentProfile> findByUserIdForUpdate(@Param("userId") UUID userId);
}
