package com.ptu.backend.repository;

import com.ptu.backend.domain.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("legacyTeacherAssignmentRepository")
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {

    boolean existsByTeacherIdAndGroupIdAndDisciplineId(Long teacherId, Long groupId, Long disciplineId);
}
