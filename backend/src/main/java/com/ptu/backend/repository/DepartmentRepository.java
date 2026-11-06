package com.ptu.backend.repository;

import com.ptu.backend.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("legacyDepartmentRepository")
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
