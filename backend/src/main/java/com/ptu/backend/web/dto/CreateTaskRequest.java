package com.ptu.backend.web.dto;

import java.time.LocalDateTime;

public record CreateTaskRequest(
    String title,
    String description,
    LocalDateTime dueDate,
    Long groupId,
    Long disciplineId
) {
}
