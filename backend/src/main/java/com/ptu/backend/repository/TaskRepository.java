package com.ptu.backend.repository;

import com.ptu.backend.domain.Task;
import com.ptu.backend.domain.TaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByGroupIdAndStatus(Long groupId, TaskStatus status);

    Optional<Task> findByIdAndTeacherId(Long id, Long teacherId);
}
