package com.ptu.backend.repository;

import com.ptu.backend.domain.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<StudentGroup, Long> {
}
