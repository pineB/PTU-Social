package com.ptu.backend.academic.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ptu.backend.academic.domain.StudyGroup;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, UUID> {

	Optional<StudyGroup> findByNameIgnoreCase(String name);

	List<StudyGroup> findAllByFacultyId(UUID facultyId, Sort sort);

	boolean existsByFacultyId(UUID facultyId);
}
