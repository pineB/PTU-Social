package com.ptu.backend.user.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptu.backend.user.domain.StudentProfile;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

	Optional<StudentProfile> findByUserId(UUID userId);

	List<StudentProfile> findAllByGroupId(UUID groupId);
}
