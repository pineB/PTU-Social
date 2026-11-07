package com.ptu.backend.auth.service;

import java.util.Optional;
import java.util.UUID;

import com.ptu.backend.auth.dto.ChangePasswordRequest;
import com.ptu.backend.auth.dto.ForgotPasswordRequest;
import com.ptu.backend.auth.dto.ResetPasswordRequest;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.config.AppProperties;
import com.ptu.backend.mail.MailService;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Password change for an authenticated user and self-service reset by a one-time email code.
 * Reset endpoints never reveal whether an account exists: the request always succeeds, and an
 * unknown email produces the same error as an invalid code.
 */
@Slf4j
@Service
public class PasswordManagementService {

	private static final String INVALID_CODE = "Неверный или просроченный код подтверждения";

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final VerificationCodeService codes;
	private final MailService mailService;
	private final AppProperties properties;

	public PasswordManagementService(UserRepository users, PasswordEncoder passwordEncoder,
			VerificationCodeService codes, MailService mailService, AppProperties properties) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.codes = codes;
		this.mailService = mailService;
		this.properties = properties;
	}

	@Transactional
	public void change(UUID userId, ChangePasswordRequest request) {
		User user = users.findById(userId)
				.orElseThrow(() -> ApiException.notFound("Учётная запись не найдена"));
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw ApiException.badRequest("Текущий пароль указан неверно");
		}
		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw ApiException.badRequest("Новый пароль должен отличаться от текущего");
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
	}

	@Transactional(readOnly = true)
	public void requestCode(ForgotPasswordRequest request) {
		findActive(request.email()).ifPresent(user -> {
			try {
				String code = codes.issueCode(VerificationCodeService.SCOPE_PASSWORD_RESET, subject(user));
				mailService.sendPasswordResetCode(user.getEmail(), user.getFullName(), code,
						properties.verification().codeTtl());
			}
			catch (ApiException ex) {
				if (ex.getStatus() != HttpStatus.TOO_MANY_REQUESTS) {
					throw ex;
				}
				log.warn("Password reset code for {} was not sent: {}", user.getEmail(), ex.getMessage());
			}
		});
	}

	@Transactional
	public void reset(ResetPasswordRequest request) {
		User user = findActive(request.email())
				.orElseThrow(() -> ApiException.badRequest(INVALID_CODE));
		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw ApiException.badRequest("Новый пароль должен отличаться от текущего");
		}
		try {
			codes.verifyCode(VerificationCodeService.SCOPE_PASSWORD_RESET, subject(user), request.code());
		}
		catch (ApiException ex) {
			if (ex.getStatus() == HttpStatus.TOO_MANY_REQUESTS) {
				throw ex;
			}
			throw ApiException.badRequest(INVALID_CODE);
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
	}

	private Optional<User> findActive(String email) {
		return users.findByEmailIgnoreCase(RegistrationService.normalizeEmail(email))
				.filter(User::isActive);
	}

	private static String subject(User user) {
		return user.getId().toString();
	}
}
