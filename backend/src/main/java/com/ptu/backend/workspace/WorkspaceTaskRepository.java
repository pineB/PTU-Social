package com.ptu.backend.workspace;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceTaskRepository extends JpaRepository<WorkspaceTask, UUID> {

	List<WorkspaceTask> findAllByGroupIdAndArchivedFalseOrderByDueAtAsc(UUID groupId);

	List<WorkspaceTask> findAllByTeacherIdAndArchivedFalseOrderByDueAtAsc(UUID teacherId);

	List<WorkspaceTask> findAllByIdIn(Collection<UUID> ids);
}
