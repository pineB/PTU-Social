package com.ptu.backend.workspace;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ptu.backend.common.security.UserPrincipal;
import com.ptu.backend.workspace.WorkspaceTaskDtos.CreateTaskRequest;
import com.ptu.backend.workspace.WorkspaceTaskDtos.ReviewRequest;
import com.ptu.backend.workspace.WorkspaceTaskDtos.SubmissionResponse;
import com.ptu.backend.workspace.WorkspaceTaskDtos.TaskResponse;
import com.ptu.backend.workspace.WorkspaceTaskDtos.TeachingAssignmentOption;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceTaskController {

	private final WorkspaceTaskService service;

	public WorkspaceTaskController(WorkspaceTaskService service) {
		this.service = service;
	}

	@GetMapping("/student/tasks")
	@PreAuthorize("hasRole('STUDENT')")
	public List<TaskResponse> listStudentTasks(@AuthenticationPrincipal UserPrincipal principal) {
		return service.listStudentTasks(principal);
	}

	@PostMapping(path = "/teacher/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestPart("request") CreateTaskRequest request,
			@RequestPart(required = false) MultipartFile file) {
		return ResponseEntity.status(201).body(service.createTask(principal, request, file));
	}

	@GetMapping("/teacher/tasks")
	@PreAuthorize("hasRole('TEACHER')")
	public List<TaskResponse> listTeacherTasks(@AuthenticationPrincipal UserPrincipal principal) {
		return service.listTeacherTasks(principal);
	}

	@GetMapping("/teacher/assignments")
	@PreAuthorize("hasRole('TEACHER')")
	public List<TeachingAssignmentOption> listTeachingAssignments(@AuthenticationPrincipal UserPrincipal principal) {
		return service.listTeachingAssignments(principal);
	}

	@PostMapping(path = "/student/tasks/{taskId}/submission", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('STUDENT')")
	public TaskResponse submit(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID taskId,
			@RequestParam(required = false) String content,
			@RequestPart(required = false) MultipartFile file) {
		return service.submit(principal, taskId, content, file);
	}

	@GetMapping("/teacher/submissions")
	@PreAuthorize("hasRole('TEACHER')")
	public List<SubmissionResponse> listTeacherSubmissions(@AuthenticationPrincipal UserPrincipal principal) {
		return service.listTeacherSubmissions(principal);
	}

	@PatchMapping("/teacher/submissions/{submissionId}")
	@PreAuthorize("hasRole('TEACHER')")
	public SubmissionResponse review(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID submissionId, @Valid @RequestBody ReviewRequest request) {
		return service.review(principal, submissionId, request);
	}

	@GetMapping("/submissions/{submissionId}/attachment")
	public ResponseEntity<byte[]> downloadAttachment(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID submissionId) {
		WorkspaceTaskService.Attachment attachment = service.attachment(principal, submissionId);
		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		if (attachment.contentType() != null) {
			try {
				mediaType = MediaType.parseMediaType(attachment.contentType());
			} catch (IllegalArgumentException ignored) {
				mediaType = MediaType.APPLICATION_OCTET_STREAM;
			}
		}
		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(attachment.fileName(), StandardCharsets.UTF_8).build().toString())
				.body(attachment.data());
	}

	@GetMapping("/tasks/{taskId}/attachment")
	public ResponseEntity<byte[]> downloadTaskAttachment(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID taskId) {
		WorkspaceTaskService.Attachment attachment = service.taskAttachment(principal, taskId);
		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		if (attachment.contentType() != null) {
			try {
				mediaType = MediaType.parseMediaType(attachment.contentType());
			} catch (IllegalArgumentException ignored) {
				mediaType = MediaType.APPLICATION_OCTET_STREAM;
			}
		}
		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(attachment.fileName(), StandardCharsets.UTF_8).build().toString())
				.body(attachment.data());
	}
}
