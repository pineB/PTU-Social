package com.ptu.backend.service;

import com.ptu.backend.domain.Discipline;
import com.ptu.backend.domain.StudentGroup;
import com.ptu.backend.domain.Task;
import com.ptu.backend.domain.TaskStatus;
import com.ptu.backend.domain.TeacherAssignment;
import com.ptu.backend.repository.DisciplineRepository;
import com.ptu.backend.repository.GroupRepository;
import com.ptu.backend.repository.TaskRepository;
import com.ptu.backend.repository.TeacherAssignmentRepository;
import com.ptu.backend.web.dto.CreateTaskRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final GroupRepository groupRepository;
    private final DisciplineRepository disciplineRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;

    @Transactional
    public Task createTask(Long teacherId, CreateTaskRequest request) {
        boolean isTeacherAssigned = teacherAssignmentRepository
            .existsByTeacherIdAndGroupIdAndDisciplineId(teacherId, request.groupId(), request.disciplineId());

        if (!isTeacherAssigned) {
            throw new IllegalArgumentException("Teacher is not assigned to the group and discipline");
        }

        StudentGroup group = groupRepository.findById(request.groupId())
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        Discipline discipline = disciplineRepository.findById(request.disciplineId())
            .orElseThrow(() -> new IllegalArgumentException("Discipline not found"));

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setTeacherId(teacherId);
        task.setGroup(group);
        task.setDiscipline(discipline);
        task.setStatus(TaskStatus.ACTIVE);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksByGroup(Long groupId) {
        return taskRepository.findByGroupIdAndStatus(groupId, TaskStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForStudent(Long studentId, Long groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group id is required to list tasks for student");
        }
        return taskRepository.findByGroupIdAndStatus(groupId, TaskStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Task getTask(Long taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }
}
