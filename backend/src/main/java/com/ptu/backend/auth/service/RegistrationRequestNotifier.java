package com.ptu.backend.auth.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.ptu.backend.academic.domain.AssignmentStatus;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.repo.SemesterRepository;
import com.ptu.backend.academic.repo.StudyGroupRepository;
import com.ptu.backend.academic.repo.TeacherAssignmentRepository;
import com.ptu.backend.auth.domain.RegistrationRequest;
import com.ptu.backend.config.AppProperties;
import com.ptu.backend.mail.MailService;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.StudentProfileRepository;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Notifies the people who review a verified student registration request:
 * teachers with an active assignment for the group in the current semester and the group leader.
 */
@Slf4j
@Service
public class RegistrationRequestNotifier {

	private static final String TEACHER_PATH = "/teacher/students";
	private static final String LEADER_PATH = "/student/group-confirmation";

	private final TeacherAssignmentRepository assignments;
	private final StudentProfileRepository profiles;
	private final SemesterRepository semesters;
	private final StudyGroupRepository groups;
	private final UserRepository users;
	private final MailService mailService;
	private final AppProperties properties;

	public RegistrationRequestNotifier(TeacherAssignmentRepository assignments, StudentProfileRepository profiles,
			SemesterRepository semesters, StudyGroupRepository groups, UserRepository users, MailService mailService,
			AppProperties properties) {
		this.assignments = assignments;
		this.profiles = profiles;
		this.semesters = semesters;
		this.groups = groups;
		this.users = users;
		this.mailService = mailService;
		this.properties = properties;
	}

	public void notifyReviewers(RegistrationRequest request) {
		String groupName = groups.findById(request.getGroupId())
				.map(StudyGroup::getName)
				.orElse("учебная группа");
		Set<UUID> recipientIds = new LinkedHashSet<>();
		semesters.findByCurrentTrue().ifPresent(semester -> recipientIds.addAll(
				assignments.findTeacherIdsByGroup(request.getGroupId(), semester.getId(), AssignmentStatus.ACTIVE)));
		profiles.findAllByGroupIdAndGroupLeaderTrue(request.getGroupId())
				.forEach(profile -> recipientIds.add(profile.getUserId()));
		if (recipientIds.isEmpty()) {
			log.warn("No reviewers found for registration request {} in group {}", request.getId(), groupName);
			return;
		}
		for (User user : users.findAllById(recipientIds)) {
			if (user.getEmail().equalsIgnoreCase(request.getEmail())) {
				continue;
			}
			String link = properties.baseUrl() + (user.getRole() == Role.TEACHER ? TEACHER_PATH : LEADER_PATH);
			try {
				mailService.sendRegistrationPendingNotice(user.getEmail(), user.getFullName(),
						request.getFullName(), groupName, link);
			}
			catch (RuntimeException ex) {
				log.warn("Failed to notify {} about registration request {}: {}", user.getEmail(), request.getId(),
						ex.getMessage());
			}
		}
	}
}
