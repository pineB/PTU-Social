package com.ptu.backend.academic.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptu.backend.academic.domain.Semester;

public interface SemesterRepository extends JpaRepository<Semester, UUID> {

	Optional<Semester> findByCurrentTrue();

	Optional<Semester> findByName(String name);
}
