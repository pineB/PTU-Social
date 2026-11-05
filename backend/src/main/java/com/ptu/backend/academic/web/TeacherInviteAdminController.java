package com.ptu.backend.academic.web;

import com.ptu.backend.auth.dto.CreateTeacherInviteRequest;
import com.ptu.backend.auth.dto.TeacherInviteCreatedResponse;
import com.ptu.backend.auth.service.TeacherInviteService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic/teacher-invites")
public class TeacherInviteAdminController {

	private final TeacherInviteService teacherInviteService;

	public TeacherInviteAdminController(TeacherInviteService teacherInviteService) {
		this.teacherInviteService = teacherInviteService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TeacherInviteCreatedResponse create(@Valid @RequestBody CreateTeacherInviteRequest request) {
		return teacherInviteService.create(request);
	}
}
