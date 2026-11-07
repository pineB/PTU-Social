package com.ptu.backend.auth.web;

import java.util.UUID;

import com.ptu.backend.auth.dto.AcceptTeacherInviteRequest;
import com.ptu.backend.auth.dto.ChangePasswordRequest;
import com.ptu.backend.auth.dto.CreateRegistrationRequest;
import com.ptu.backend.auth.dto.ForgotPasswordRequest;
import com.ptu.backend.auth.dto.LoginRequest;
import com.ptu.backend.auth.dto.RegistrationRequestView;
import com.ptu.backend.auth.dto.ResetPasswordRequest;
import com.ptu.backend.auth.dto.TeacherInviteInfoResponse;
import com.ptu.backend.auth.dto.TokenResponse;
import com.ptu.backend.auth.dto.VerifyEmailRequest;
import com.ptu.backend.auth.service.AuthService;
import com.ptu.backend.auth.service.PasswordManagementService;
import com.ptu.backend.auth.service.RegistrationService;
import com.ptu.backend.auth.service.TeacherInviteService;
import com.ptu.backend.common.security.UserPrincipal;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final RegistrationService registrationService;
	private final TeacherInviteService teacherInviteService;
	private final AuthService authService;
	private final PasswordManagementService passwordManagementService;

	public AuthController(RegistrationService registrationService, TeacherInviteService teacherInviteService,
			AuthService authService, PasswordManagementService passwordManagementService) {
		this.registrationService = registrationService;
		this.teacherInviteService = teacherInviteService;
		this.authService = authService;
		this.passwordManagementService = passwordManagementService;
	}

	@PostMapping("/registration-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistrationRequestView createRegistrationRequest(
			@Valid @RequestBody CreateRegistrationRequest request) {
		return registrationService.create(request);
	}

	@PostMapping("/registration-requests/{requestId}/email-code")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void resendCode(@PathVariable UUID requestId) {
		registrationService.resendCode(requestId);
	}

	@PostMapping("/registration-requests/{requestId}/email-verify")
	public RegistrationRequestView verifyEmail(@PathVariable UUID requestId,
			@Valid @RequestBody VerifyEmailRequest request) {
		return registrationService.verifyEmail(requestId, request.code());
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/teacher-invites/{token}")
	public TeacherInviteInfoResponse teacherInviteInfo(@PathVariable String token) {
		return teacherInviteService.info(token);
	}

	@PostMapping("/teacher-invites/{token}/accept")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse acceptTeacherInvite(@PathVariable String token,
			@Valid @RequestBody AcceptTeacherInviteRequest request) {
		return teacherInviteService.accept(token, request);
	}

	@PostMapping("/password/change")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ChangePasswordRequest request) {
		passwordManagementService.change(principal.id(), request);
	}

	@PostMapping("/password/forgot")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		passwordManagementService.requestCode(request);
	}

	@PostMapping("/password/reset")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		passwordManagementService.reset(request);
	}
}
