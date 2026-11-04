package com.ptu.backend.academic.web;

import java.util.UUID;

import com.ptu.backend.auth.dto.RegistrationRequestView;
import com.ptu.backend.auth.service.RegistrationApprovalService;
import com.ptu.backend.common.security.UserPrincipal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic/registration-requests")
public class RegistrationApprovalController {

	private static final int MAX_PAGE_SIZE = 100;

	private final RegistrationApprovalService approvalService;

	public RegistrationApprovalController(RegistrationApprovalService approvalService) {
		this.approvalService = approvalService;
	}

	@GetMapping
	public Page<RegistrationRequestView> listPending(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));
		return approvalService.listPending(principal, pageable);
	}

	@PostMapping("/{requestId}/approve")
	public RegistrationRequestView approve(@PathVariable UUID requestId,
			@AuthenticationPrincipal UserPrincipal principal) {
		return approvalService.approve(requestId, principal);
	}

	@PostMapping("/{requestId}/reject")
	public RegistrationRequestView reject(@PathVariable UUID requestId,
			@AuthenticationPrincipal UserPrincipal principal) {
		return approvalService.reject(requestId, principal);
	}
}
