package com.ptu.backend.academic.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptu.backend.academic.domain.Discipline;

public interface DisciplineRepository extends JpaRepository<Discipline, UUID> {

	Optional<Discipline> findByNameIgnoreCaseAndDepartmentId(String name, UUID departmentId);
}
