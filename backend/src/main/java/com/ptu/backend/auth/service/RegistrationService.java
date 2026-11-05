package com.ptu.backend.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ptu.backend.academic.domain.Faculty;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.repo.FacultyRepository;
import com.ptu.backend.academic.repo.StudyGroupRepository;
import com.ptu.backend.auth.domain.RegistrationRequest;
import com.ptu.backend.auth.domain.RegistrationStatus;
import com.ptu.backend.auth.dto.CreateRegistrationRequest;
import com.ptu.backend.auth.dto.RegistrationRequestView;
import com.ptu.backend.auth.repo.RegistrationRequestRepository;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.config.AppProperties;
import com.ptu.backend.mail.MailService;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RegistrationService {

	private static final List<RegistrationStatus> ACTIVE_STATUSES = List.of(
			RegistrationStatus.PENDING_EMAIL, RegistrationStatus.EMAIL_VERIFIED);

	private final RegistrationRequestRepository requests;
	private final FacultyRepository faculties;
	private final StudyGroupRepository groups;
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationService emailVerification;
	private final MailService mailService;
	private final AppProperties properties;

	public RegistrationService(RegistrationRequestRepository requests, FacultyRepository faculties,
			StudyGroupRepository groups, UserRepository users, PasswordEncoder passwordEncoder,
			EmailVerificationService emailVerification, MailService mailService, AppProperties properties) {
		this.requests = requests;
		this.faculties = faculties;
		this.groups = groups;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.emailVerification = emailVerification;
		this.mailService = mailService;
		this.properties = properties;
	}

	@Transactional
	public RegistrationRequestView create(CreateRegistrationRequest request) {
		String email = normalizeEmail(request.email());
		Faculty faculty = faculties.findById(request.facultyId())
				.orElseThrow(() -> ApiException.badRequest("Факультет не найден"));
		StudyGroup group = groups.findById(request.groupId())
				.orElseThrow(() -> ApiException.badRequest("Учебная группа не найдена"));
		if (!group.getFacultyId().equals(faculty.getId())) {
			throw ApiException.badRequest("Учебная группа не относится к выбранному факультету");
		}
		if (users.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("Пользователь с таким email уже существует");
		}
		if (requests.existsByEmailIgnoreCaseAndStatusIn(email, ACTIVE_STATUSES)) {
			throw ApiException.conflict("Заявка с таким email уже существует");
		}

		RegistrationRequest entity = new RegistrationRequest();
		entity.setEmail(email);
		entity.setFullName(request.fullName().trim());
		entity.setPasswordHash(passwordEncoder.encode(request.password()));
		entity.setFacultyId(faculty.getId());
		entity.setGroupId(group.getId());
		entity.setStatus(RegistrationStatus.PENDING_EMAIL);
		entity.setExpiresAt(Instant.now().plus(properties.registration().ttl()));
		try {
			requests.saveAndFlush(entity);
		}
		catch (DataIntegrityViolationException ex) {
			throw ApiException.conflict("Заявка с таким email уже существует");
		}

		sendCode(entity);
		return RegistrationRequestView.from(entity);
	}

	@Transactional
	public void resendCode(UUID requestId) {
		RegistrationRequest request = findRequest(requestId);
		if (request.getStatus() != RegistrationStatus.PENDING_EMAIL) {
			throw ApiException.conflict("Подтверждение email для заявки недоступно");
		}
		ensureNotExpired(request);
		sendCode(request);
	}

	@Transactional
	public RegistrationRequestView verifyEmail(UUID requestId, String code) {
		RegistrationRequest request = findRequest(requestId);
		if (request.getStatus() != RegistrationStatus.PENDING_EMAIL) {
			throw ApiException.conflict("Заявка уже подтверждена или обработана");
		}
		ensureNotExpired(request);
		emailVerification.verifyCode(requestId, code);
		request.setStatus(RegistrationStatus.EMAIL_VERIFIED);
		request.setEmailVerifiedAt(Instant.now());
		return RegistrationRequestView.from(request);
	}

	private void sendCode(RegistrationRequest request) {
		String code = emailVerification.issueCode(request.getId());
		mailService.sendVerificationCode(request.getEmail(), request.getFullName(), code,
				properties.verification().codeTtl());
	}

	private RegistrationRequest findRequest(UUID requestId) {
		return requests.findById(requestId)
				.orElseThrow(() -> ApiException.notFound("Заявка на регистрацию не найдена"));
	}

	private static void ensureNotExpired(RegistrationRequest request) {
		if (request.getExpiresAt().isBefore(Instant.now())) {
			throw ApiException.gone("Срок действия заявки истёк, создайте новую");
		}
	}

	public static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase();
	}
}
