package com.ptu.backend.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.ptu.backend.academic.domain.Semester;
import com.ptu.backend.academic.domain.TeacherAssignment;
import com.ptu.backend.academic.repo.DepartmentRepository;
import com.ptu.backend.academic.repo.DisciplineRepository;
import com.ptu.backend.academic.repo.SemesterRepository;
import com.ptu.backend.academic.repo.StudyGroupRepository;
import com.ptu.backend.academic.repo.TeacherAssignmentRepository;
import com.ptu.backend.auth.domain.TeacherInvite;
import com.ptu.backend.auth.domain.TeacherInviteAssignment;
import com.ptu.backend.auth.domain.TeacherInviteAssignmentId;
import com.ptu.backend.auth.domain.TeacherInviteStatus;
import com.ptu.backend.auth.dto.AcceptTeacherInviteRequest;
import com.ptu.backend.auth.dto.CreateTeacherInviteRequest;
import com.ptu.backend.auth.dto.TeacherInviteCreatedResponse;
import com.ptu.backend.auth.dto.TeacherInviteInfoResponse;
import com.ptu.backend.auth.dto.TokenResponse;
import com.ptu.backend.auth.repo.TeacherInviteAssignmentRepository;
import com.ptu.backend.auth.repo.TeacherInviteRepository;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.security.JwtService;
import com.ptu.backend.common.util.Hashing;
import com.ptu.backend.config.AppProperties;
import com.ptu.backend.mail.MailService;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherInviteService {

	private final TeacherInviteRepository invites;
	private final TeacherInviteAssignmentRepository inviteAssignments;
	private final DepartmentRepository departments;
	private final DisciplineRepository disciplines;
	private final StudyGroupRepository groups;
	private final SemesterRepository semesters;
	private final TeacherAssignmentRepository teacherAssignments;
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final MailService mailService;
	private final JwtService jwtService;
	private final AppProperties properties;

	public TeacherInviteService(TeacherInviteRepository invites,
			TeacherInviteAssignmentRepository inviteAssignments, DepartmentRepository departments,
			DisciplineRepository disciplines, StudyGroupRepository groups, SemesterRepository semesters,
			TeacherAssignmentRepository teacherAssignments, UserRepository users, PasswordEncoder passwordEncoder,
			MailService mailService, JwtService jwtService, AppProperties properties) {
		this.invites = invites;
		this.inviteAssignments = inviteAssignments;
		this.departments = departments;
		this.disciplines = disciplines;
		this.groups = groups;
		this.semesters = semesters;
		this.teacherAssignments = teacherAssignments;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.mailService = mailService;
		this.jwtService = jwtService;
		this.properties = properties;
	}

	@Transactional
	public TeacherInviteCreatedResponse create(CreateTeacherInviteRequest request) {
		String email = RegistrationService.normalizeEmail(request.email());
		departments.findById(request.departmentId())
				.orElseThrow(() -> ApiException.badRequest("Кафедра не найдена"));
		if (users.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("Пользователь с таким email уже существует");
		}
		if (invites.existsByEmailIgnoreCaseAndStatus(email, TeacherInviteStatus.PENDING)) {
			throw ApiException.conflict("Активное приглашение для этого email уже существует");
		}
		List<UUID> disciplineIds = List.copyOf(request.disciplineIds());
		if (disciplines.findAllById(disciplineIds).size() != disciplineIds.size()) {
			throw ApiException.badRequest("Некоторые дисциплины не найдены");
		}
		for (UUID groupId : request.groupIds()) {
			if (!groups.existsById(groupId)) {
				throw ApiException.badRequest("Учебная группа не найдена: " + groupId);
			}
		}

		String token = Hashing.randomToken();
		TeacherInvite invite = new TeacherInvite();
		invite.setEmail(email);
		invite.setFullName(request.fullName().trim());
		invite.setDepartmentId(request.departmentId());
		invite.setTokenHash(Hashing.sha256Hex(token));
		invite.setStatus(TeacherInviteStatus.PENDING);
		invite.setExpiresAt(Instant.now().plus(properties.invite().ttl()));
		invites.saveAndFlush(invite);

		Set<TeacherInviteAssignmentId> ids = new HashSet<>();
		for (UUID disciplineId : disciplineIds) {
			for (UUID groupId : request.groupIds()) {
				ids.add(new TeacherInviteAssignmentId(invite.getId(), disciplineId, groupId));
			}
		}
		inviteAssignments.saveAll(ids.stream().map(TeacherInviteAssignment::new).toList());

		String link = "%s/accept-invite?token=%s".formatted(
				properties.baseUrl(),
				URLEncoder.encode(token, StandardCharsets.UTF_8));
		mailService.sendTeacherInvite(email, invite.getFullName(), link, properties.invite().ttl());
		return new TeacherInviteCreatedResponse(invite.getId(), email, invite.getExpiresAt());
	}

	@Transactional(readOnly = true)
	public TeacherInviteInfoResponse info(String token) {
		TeacherInvite invite = findPending(token);
		return new TeacherInviteInfoResponse(invite.getFullName(), invite.getEmail(), invite.getExpiresAt());
	}

	@Transactional
	public TokenResponse accept(String token, AcceptTeacherInviteRequest request) {
		TeacherInvite invite = findPending(token);
		if (users.existsByEmailIgnoreCase(invite.getEmail())) {
			throw ApiException.conflict("Пользователь с таким email уже существует");
		}
		Semester semester = semesters.findByCurrentTrue()
				.orElseThrow(() -> ApiException.conflict("Текущий семестр не настроен"));

		User teacher = new User();
		teacher.setEmail(invite.getEmail());
		teacher.setFullName(invite.getFullName());
		teacher.setPasswordHash(passwordEncoder.encode(request.password()));
		teacher.setRole(Role.TEACHER);
		users.saveAndFlush(teacher);

		List<TeacherInviteAssignment> assignments = inviteAssignments.findAllByIdInviteId(invite.getId());
		for (TeacherInviteAssignment assignment : assignments) {
			UUID disciplineId = assignment.getId().getDisciplineId();
			UUID groupId = assignment.getId().getGroupId();
			if (!teacherAssignments.existsByTeacherIdAndGroupIdAndSemesterId(teacher.getId(), groupId,
					semester.getId())) {
				teacherAssignments.save(new TeacherAssignment(teacher.getId(), disciplineId, groupId,
						semester.getId()));
			}
		}

		invite.setStatus(TeacherInviteStatus.ACCEPTED);
		invite.setAcceptedAt(Instant.now());
		return new TokenResponse(jwtService.generate(teacher), "Bearer", jwtService.ttlSeconds(), teacher.getId(),
				teacher.getRole());
	}

	private TeacherInvite findPending(String token) {
		TeacherInvite invite = invites.findByTokenHash(Hashing.sha256Hex(token))
				.orElseThrow(() -> ApiException.notFound("Приглашение не найдено"));
		if (invite.getStatus() != TeacherInviteStatus.PENDING) {
			throw ApiException.gone("Приглашение уже использовано или отозвано");
		}
		if (invite.getExpiresAt().isBefore(Instant.now())) {
			throw ApiException.gone("Срок действия приглашения истёк");
		}
		return invite;
	}
}
