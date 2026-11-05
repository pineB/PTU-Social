package com.ptu.backend.user.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ptu.backend.academic.domain.Semester;
import com.ptu.backend.academic.domain.TeacherAssignment;
import com.ptu.backend.academic.repo.SemesterRepository;
import com.ptu.backend.academic.repo.StudyGroupRepository;
import com.ptu.backend.academic.repo.TeacherAssignmentRepository;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.security.UserPrincipal;
import com.ptu.backend.user.domain.StudentProfile;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.StudentProfileRepository;
import com.ptu.backend.user.repo.UserRepository;

@RestController
@RequestMapping("/api")
public class CurrentUserController {

	private final UserRepository users;
	private final StudentProfileRepository studentProfiles;
	private final StudyGroupRepository groups;
	private final SemesterRepository semesters;
	private final TeacherAssignmentRepository teacherAssignments;

	public CurrentUserController(UserRepository users, StudentProfileRepository studentProfiles,
			StudyGroupRepository groups, SemesterRepository semesters,
			TeacherAssignmentRepository teacherAssignments) {
		this.users = users;
		this.studentProfiles = studentProfiles;
		this.groups = groups;
		this.semesters = semesters;
		this.teacherAssignments = teacherAssignments;
	}

	@GetMapping("/me")
	public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
		User user = users.findById(principal.id())
				.orElseThrow(() -> ApiException.notFound("Учётная запись не найдена"));
		StudentProfile profile = studentProfiles.findByUserId(user.getId()).orElse(null);
		String groupName = profile == null ? null : groups.findById(profile.getGroupId())
				.map(group -> group.getName())
				.orElse(null);
		List<TeachingGroup> assignedGroups = semesters.findByCurrentTrue()
				.filter(semester -> user.getRole().name().equals("TEACHER"))
				.map(semester -> teacherAssignments.findAllByTeacherIdAndSemesterId(user.getId(), semester.getId())
						.stream()
						.map(TeacherAssignment::getGroupId)
						.distinct()
						.map(groupId -> groups.findById(groupId)
								.map(group -> new TeachingGroup(group.getId(), group.getName()))
								.orElse(null))
						.filter(group -> group != null)
						.toList())
				.orElseGet(List::of);
		return new CurrentUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(),
				profile == null ? null : profile.getGroupId(), groupName,
				profile != null && profile.isGroupLeader(),
				profile == null ? null : profile.getAcademicRating(),
				profile == null ? null : profile.getBoaPoints(),
				profile == null ? null : profile.getBuaPoints(),
				assignedGroups);
	}

	public record TeachingGroup(UUID id, String name) {
	}

	public record CurrentUserResponse(UUID id, String email, String fullName, String role, UUID groupId,
			String groupName, boolean groupLeader, Integer academicRating, Integer boaPoints, Integer buaPoints,
			List<TeachingGroup> teachingGroups) {
	}
}
