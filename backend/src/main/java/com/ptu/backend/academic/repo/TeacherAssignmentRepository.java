package com.ptu.backend.academic.repo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ptu.backend.academic.domain.AssignmentStatus;
import com.ptu.backend.academic.domain.TeacherAssignment;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, UUID> {

	boolean existsByTeacherIdAndGroupIdAndSemesterId(UUID teacherId, UUID groupId, UUID semesterId);

	boolean existsByTeacherIdAndGroupIdAndDisciplineIdAndSemesterId(UUID teacherId, UUID groupId,
			UUID disciplineId, UUID semesterId);

	boolean existsByTeacherIdAndGroupIdAndDisciplineIdAndSemesterIdAndStatus(UUID teacherId, UUID groupId,
			UUID disciplineId, UUID semesterId, AssignmentStatus status);

	@Query("""
			select distinct a.groupId from TeacherAssignment a
			where a.teacherId = :teacherId and a.semesterId = :semesterId and a.status = :status
			""")
	List<UUID> findGroupIds(@Param("teacherId") UUID teacherId,
			@Param("semesterId") UUID semesterId,
			@Param("status") AssignmentStatus status);

	List<TeacherAssignment> findAllByTeacherIdAndSemesterId(UUID teacherId, UUID semesterId);

	List<TeacherAssignment> findAllByTeacherIdInAndSemesterId(Collection<UUID> teacherIds, UUID semesterId);
}
