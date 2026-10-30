package com.ptu.backend.academic.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptu.backend.academic.domain.Faculty;

public interface FacultyRepository extends JpaRepository<Faculty, UUID> {

	Optional<Faculty> findByNameIgnoreCase(String name);
}
