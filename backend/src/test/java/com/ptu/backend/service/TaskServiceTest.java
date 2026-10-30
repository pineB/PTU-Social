package com.ptu.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ptu.backend.domain.Task;
import com.ptu.backend.domain.TaskStatus;
import com.ptu.backend.repository.DisciplineRepository;
import com.ptu.backend.repository.GroupRepository;
import com.ptu.backend.repository.TaskRepository;
import com.ptu.backend.repository.TeacherAssignmentRepository;
import com.ptu.backend.web.dto.CreateTaskRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private DisciplineRepository disciplineRepository;

    @Mock
    private TeacherAssignmentRepository teacherAssignmentRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void shouldReturnTasksForStudentGroup() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Task 1");
        task.setStatus(TaskStatus.ACTIVE);

        when(taskRepository.findByGroupIdAndStatus(12L, TaskStatus.ACTIVE)).thenReturn(List.of(task));

        List<Task> tasks = taskService.getTasksForStudent(99L, 12L);

        assertEquals(1, tasks.size());
        assertEquals("Task 1", tasks.get(0).getTitle());
    }

    @Test
    void shouldRejectTaskCreationWhenTeacherHasNoAssignment() {
        CreateTaskRequest request = new CreateTaskRequest(
            "Task 2",
            "Description",
            LocalDateTime.now().plusDays(5),
            7L,
            3L
        );

        when(teacherAssignmentRepository.existsByTeacherIdAndGroupIdAndDisciplineId(10L, 7L, 3L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> taskService.createTask(10L, request)
        );

        assertEquals("Teacher is not assigned to the group and discipline", exception.getMessage());
    }
}
