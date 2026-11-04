package com.ptu.backend.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ptu.backend.academic.domain.AssignmentStatus;
import com.ptu.backend.academic.repo.SemesterRepository;
import com.ptu.backend.academic.repo.TeacherAssignmentRepository;
import com.ptu.backend.auth.domain.RegistrationRequest;
import com.ptu.backend.auth.domain.RegistrationStatus;
import com.ptu.backend.auth.dto.RegistrationRequestView;
import com.ptu.backend.auth.repo.RegistrationRequestRepository;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.security.UserPrincipal;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.StudentProfile;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.StudentProfileRepository;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationApprovalService {

	private final RegistrationRequestRepository requests;
	private final UserRepository users;
	private final StudentProfileRepository profiles;
	private final TeacherAssignmentRepository assignments;
	private final SemesterRepository semesters;
	private final EmailVerificationService emailVerification;

	public RegistrationApprovalService(RegistrationRequestRepository requests, UserRepository users,
			StudentProfileRepository profiles, TeacherAssignmentRepository assignments,
			SemesterRepository semesters, EmailVerificationService emailVerification) {
		this.requests = requests;
		this.users = users;
		this.profiles = profiles;
		this.assignments = assignments;
		this.semesters = semesters;
		this.emailVerification = emailVerification;
	}

	@Transactional(readOnly = true)
	public Page<RegistrationRequestView> listPending(UserPrincipal principal, Pageable pageable) {
		if (principal.role() == Role.ACADEMIC_OFFICE) {
			return requests.findByStatus(RegistrationStatus.EMAIL_VERIFIED, pageable)
					.map(RegistrationRequestView::from);
		}
		List<UUID> groupIds = manageableGroupIds(principal);
		if (groupIds.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, 0);
		}
		return requests.findByStatusAndGroupIdIn(RegistrationStatus.EMAIL_VERIFIED, groupIds, pageable)
				.map(RegistrationRequestView::from);
	}

	@Transactional
	public RegistrationRequestView approve(UUID requestId, UserPrincipal principal) {
		RegistrationRequest request = lockRequest(requestId);
		ensureCanManage(principal, request.getGroupId());
		if (request.getStatus() != RegistrationStatus.EMAIL_VERIFIED) {
			throw ApiException.conflict("Заявка не подтвердила email или уже обработана");
		}
		if (request.getExpiresAt().isBefore(Instant.now()) || request.getPasswordHash() == null) {
			request.setStatus(RegistrationStatus.EXPIRED);
			request.setPasswordHash(null);
			throw ApiException.gone("Срок действия заявки истёк");
		}
		if (users.existsByEmailIgnoreCase(request.getEmail())) {
			throw ApiException.conflict("Пользователь с таким email уже существует");
		}

		User student = new User();
		student.setEmail(request.getEmail());
		student.setFullName(request.getFullName());
		student.setPasswordHash(request.getPasswordHash());
		student.setRole(Role.STUDENT);
		try {
			users.saveAndFlush(student);
		}
		catch (DataIntegrityViolationException ex) {
			throw ApiException.conflict("Пользователь с таким email уже существует");
		}

		StudentProfile profile = new StudentProfile();
		profile.setUserId(student.getId());
		profile.setGroupId(request.getGroupId());
		profile.setGroupLeader(false);
		profiles.save(profile);

		request.setStatus(RegistrationStatus.APPROVED);
		return RegistrationRequestView.from(request);
	}

	@Transactional
	public RegistrationRequestView reject(UUID requestId, UserPrincipal principal) {
		RegistrationRequest request = lockRequest(requestId);
		ensureCanManage(principal, request.getGroupId());
		if (request.getStatus() != RegistrationStatus.PENDING_EMAIL
				&& request.getStatus() != RegistrationStatus.EMAIL_VERIFIED) {
			throw ApiException.conflict("Заявка уже обработана");
		}
		request.setStatus(RegistrationStatus.REJECTED);
		request.setPasswordHash(null);
		emailVerification.invalidate(requestId);
		return RegistrationRequestView.from(request);
	}

	private RegistrationRequest lockRequest(UUID requestId) {
		return requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> ApiException.notFound("Заявка на регистрацию не найдена"));
	}

	private void ensureCanManage(UserPrincipal principal, UUID groupId) {
		if (principal.role() == Role.ACADEMIC_OFFICE) {
			return;
		}
		if (!manageableGroupIds(principal).contains(groupId)) {
			throw ApiException.forbidden("Нет прав на обработку заявок этой группы");
		}
	}

	private List<UUID> manageableGroupIds(UserPrincipal principal) {
		return switch (principal.role()) {
			case ACADEMIC_OFFICE -> List.of();
			case TEACHER -> semesters.findByCurrentTrue()
					.map(semester -> assignments.findGroupIds(principal.id(), semester.getId(),
							AssignmentStatus.ACTIVE))
					.orElseGet(List::of);
			case STUDENT -> profiles.findByUserId(principal.id())
					.filter(StudentProfile::isGroupLeader)
					.map(profile -> List.of(profile.getGroupId()))
					.orElseGet(List::of);
			default -> List.of();
		};
	}
}
