package com.ptu.backend.user.web;

import java.util.List;

import com.ptu.backend.user.dto.CreateAcademicOfficeUserRequest;
import com.ptu.backend.user.dto.ManagedUserView;
import com.ptu.backend.user.service.AdminUserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/academic-office-users")
public class AdminUserController {

	private final AdminUserService adminUserService;

	public AdminUserController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@GetMapping
	public List<ManagedUserView> list() {
		return adminUserService.listAcademicOfficeUsers();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ManagedUserView create(@Valid @RequestBody CreateAcademicOfficeUserRequest request) {
		return adminUserService.createAcademicOfficeUser(request);
	}
}
