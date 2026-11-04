package com.ptu.backend.auth.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptu.backend.auth.domain.TeacherInviteAssignment;
import com.ptu.backend.auth.domain.TeacherInviteAssignmentId;

public interface TeacherInviteAssignmentRepository extends JpaRepository<TeacherInviteAssignment, TeacherInviteAssignmentId> {

	List<TeacherInviteAssignment> findAllByIdInviteId(UUID inviteId);
}
