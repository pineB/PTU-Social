package com.ptu.backend.workspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class WorkspaceTaskDtos {

	private WorkspaceTaskDtos() {
	}

	public record CreateTaskRequest(@NotNull UUID groupId, @NotNull UUID disciplineId,
			@NotBlank String title, @NotBlank String description, @NotNull Instant dueAt,
			@Min(1) @Max(100) int maxPoints, boolean allowLateSubmission) {
	}

	public record ReviewRequest(@NotNull WorkspaceSubmissionStatus status,
			@Min(2) @Max(5) Integer grade, String feedback) {
	}

	public record TaskResponse(UUID id, String title, String subject, String teacher, Instant dueAt, int maxPoints,
			String description, boolean allowLateSubmission, String status, Instant submittedAt, String workText,
			String fileName, String teacherComment, Integer grade, Integer awardedPoints, String attachmentUrl) {
	}

	public record SubmissionResponse(UUID id, UUID assignmentId, String assignmentTitle, String subject,
			String studentName, String group, Instant submittedAt, String workText, String fileName, String status,
			String teacherComment, Integer grade, Integer awardedPoints, String attachmentUrl) {
	}

	public record TeachingAssignmentOption(UUID groupId, String groupName, UUID disciplineId, String disciplineName) {
	}
}
