package com.ptu.backend.academic.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptu.backend.academic.domain.Department;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

	Optional<Department> findByNameIgnoreCase(String name);
}
