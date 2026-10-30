package com.ptu.backend.web.controller;

import com.ptu.backend.domain.Task;
import com.ptu.backend.service.TaskService;
import com.ptu.backend.web.dto.CreateTaskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Operations with academic tasks")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Create a task for a group")
    @PostMapping("/tasks")
    public ResponseEntity<Task> createTask(
        @RequestParam Long teacherId,
        @RequestBody CreateTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.createTask(teacherId, request));
    }

    @Operation(summary = "Get active tasks for a group")
    @GetMapping("/groups/{groupId}/tasks")
    public ResponseEntity<List<Task>> getTasksForGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(taskService.getTasksByGroup(groupId));
    }

    @Operation(summary = "Get task by id")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Task> getTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTask(taskId));
    }
}
