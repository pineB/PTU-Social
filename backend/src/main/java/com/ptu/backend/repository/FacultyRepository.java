package com.ptu.backend.repository;

import com.ptu.backend.domain.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("legacyFacultyRepository")
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
}
