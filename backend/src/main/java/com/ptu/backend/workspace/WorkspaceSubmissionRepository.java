package com.ptu.backend.workspace;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceSubmissionRepository extends JpaRepository<WorkspaceSubmission, UUID> {

	Optional<WorkspaceSubmission> findByTaskIdAndStudentId(UUID taskId, UUID studentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select submission from WorkspaceSubmission submission where submission.taskId = :taskId and submission.studentId = :studentId")
	Optional<WorkspaceSubmission> findByTaskIdAndStudentIdForUpdate(@Param("taskId") UUID taskId,
			@Param("studentId") UUID studentId);

	List<WorkspaceSubmission> findAllByTaskIdIn(Collection<UUID> taskIds);

	List<WorkspaceSubmission> findAllByTaskIdInOrderBySubmittedAtDesc(List<UUID> taskIds);

	Optional<WorkspaceSubmission> findByIdAndTaskId(UUID id, UUID taskId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select submission from WorkspaceSubmission submission where submission.id = :id")
	Optional<WorkspaceSubmission> findByIdForUpdate(@Param("id") UUID id);
}
