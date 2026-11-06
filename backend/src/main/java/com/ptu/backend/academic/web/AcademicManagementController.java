package com.ptu.backend.academic.web;

import java.util.List;
import java.util.UUID;

import com.ptu.backend.academic.service.AcademicManagementService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic/management")
@PreAuthorize("hasRole('ACADEMIC_OFFICE')")
public class AcademicManagementController {

	private final AcademicManagementService service;

	public AcademicManagementController(AcademicManagementService service) {
		this.service = service;
	}

	@GetMapping("/departments")
	public List<CatalogItem> departments() {
		return service.departments();
	}

	@GetMapping("/departments/{departmentId}/disciplines")
	public List<CatalogItem> disciplines(@PathVariable UUID departmentId) {
		return service.disciplines(departmentId);
	}

	@GetMapping("/groups")
	public List<ManagedGroup> groups() {
		return service.groups();
	}

	@PostMapping("/groups")
	@ResponseStatus(HttpStatus.CREATED)
	public ManagedGroup createGroup(@Valid @RequestBody CreateGroupRequest request) {
		return service.createGroup(request);
	}

	@PatchMapping("/groups/{groupId}")
	public ManagedGroup renameGroup(@PathVariable UUID groupId, @Valid @RequestBody RenameGroupRequest request) {
		return service.renameGroup(groupId, request);
	}

	@GetMapping("/teachers")
	public List<ManagedTeacher> teachers() {
		return service.teachers();
	}

	@PatchMapping("/teachers/{teacherId}/active")
	public ManagedTeacher setTeacherActive(@PathVariable UUID teacherId,
			@Valid @RequestBody SetActiveRequest request) {
		return service.setTeacherActive(teacherId, request);
	}

	@PostMapping("/teachers/{teacherId}/assignments")
	@ResponseStatus(HttpStatus.CREATED)
	public ManagedAssignment assignTeacher(@PathVariable UUID teacherId,
			@Valid @RequestBody CreateAssignmentRequest request) {
		return service.assignTeacher(teacherId, request);
	}

	@DeleteMapping("/teachers/{teacherId}/assignments/{assignmentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void archiveAssignment(@PathVariable UUID teacherId, @PathVariable UUID assignmentId) {
		service.archiveAssignment(teacherId, assignmentId);
	}

	public record CatalogItem(UUID id, String name) {
	}

	public record ManagedGroup(UUID id, String name, UUID facultyId, String facultyName) {
	}

	public record ManagedAssignment(UUID id, UUID groupId, String groupName, UUID disciplineId,
			String disciplineName) {
	}

	public record ManagedTeacher(UUID id, String fullName, String email, boolean active,
			List<ManagedAssignment> assignments) {
	}

	public record CreateGroupRequest(@NotBlank @Size(max = 255) String name, @NotNull UUID facultyId) {
	}

	public record RenameGroupRequest(@NotBlank @Size(max = 255) String name) {
	}

	public record SetActiveRequest(boolean active) {
	}

	public record CreateAssignmentRequest(@NotNull UUID groupId, @NotNull UUID disciplineId) {
	}
}
