package com.ptu.backend.community;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.repo.StudyGroupRepository;
import com.ptu.backend.common.security.UserPrincipal;
import com.ptu.backend.domain.Activity;
import com.ptu.backend.domain.ActivityRepository;
import com.ptu.backend.domain.Participation;
import com.ptu.backend.domain.ParticipationRepository;
import com.ptu.backend.user.domain.StudentProfile;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.StudentProfileRepository;
import com.ptu.backend.user.repo.UserRepository;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

	private final StudentProfileRepository profiles;
	private final UserRepository users;
	private final StudyGroupRepository groups;
	private final ParticipationRepository participations;
	private final ActivityRepository activities;

	public CommunityController(StudentProfileRepository profiles, UserRepository users, StudyGroupRepository groups,
			ParticipationRepository participations, ActivityRepository activities) {
		this.profiles = profiles;
		this.users = users;
		this.groups = groups;
		this.participations = participations;
		this.activities = activities;
	}

	@GetMapping("/students")
	public List<StudentView> listStudents(@AuthenticationPrincipal UserPrincipal principal) {
		List<StudentProfile> studentProfiles = profiles.findAll();
		Map<UUID, User> usersById = users.findAllById(studentProfiles.stream().map(StudentProfile::getUserId).toList())
				.stream().collect(Collectors.toMap(User::getId, Function.identity()));
		Map<UUID, StudyGroup> groupsById = groups.findAllById(studentProfiles.stream().map(StudentProfile::getGroupId).distinct().toList())
				.stream().collect(Collectors.toMap(StudyGroup::getId, Function.identity()));
		return studentProfiles.stream()
				.filter(profile -> usersById.containsKey(profile.getUserId()))
				.filter(profile -> groupsById.containsKey(profile.getGroupId()))
				.map(profile -> {
					User user = usersById.get(profile.getUserId());
					StudyGroup group = groupsById.get(profile.getGroupId());
					return new StudentView(user.getId(), user.getFullName(), group.getName(),
							profile.getAcademicRating(), profile.getBoaPoints(), profile.getBuaPoints(),
							user.getCreatedAt());
				})
				.toList();
	}

	@GetMapping("/awards")
	public List<AwardView> listAwards(@AuthenticationPrincipal UserPrincipal principal) {
		List<Participation> awarded = participations.findAll().stream()
				.filter(participation -> isPositive(participation.getPointsBoa())
						|| isPositive(participation.getPointsBua()))
				.toList();
		Map<UUID, User> usersById = users.findAllById(awarded.stream().map(Participation::getStudentId).distinct().toList())
				.stream().collect(Collectors.toMap(User::getId, Function.identity()));
		Map<Long, Activity> activitiesById = activities.findAllById(awarded.stream().map(Participation::getActivityId).distinct().toList())
				.stream().collect(Collectors.toMap(Activity::getId, Function.identity()));

		List<AwardView> result = new ArrayList<>();
		for (Participation participation : awarded) {
			User user = usersById.get(participation.getStudentId());
			Activity activity = activitiesById.get(participation.getActivityId());
			if (user == null || activity == null) continue;
			if (isPositive(participation.getPointsBoa())) {
				result.add(toAward(participation, user, activity, "БОА", participation.getPointsBoa().intValue()));
			}
			if (isPositive(participation.getPointsBua())) {
				result.add(toAward(participation, user, activity, "БУА", participation.getPointsBua().intValue()));
			}
		}
		return result.stream().sorted((left, right) -> right.createdAt().compareTo(left.createdAt())).toList();
	}

	private AwardView toAward(Participation participation, User user, Activity activity, String metric, int points) {
		String group = profiles.findByUserId(user.getId())
				.flatMap(profile -> groups.findById(profile.getGroupId()))
				.map(StudyGroup::getName)
				.orElse("");
		return new AwardView(participation.getId() + "-" + metric, user.getId(), user.getFullName(), group,
				activity.getTitle(), "Участие в мероприятии", metric, points,
				participation.getUpdatedAt().toInstant(ZoneOffset.UTC));
	}

	private boolean isPositive(BigDecimal points) {
		return points != null && points.signum() > 0;
	}

	public record StudentView(UUID id, String name, String group, int academicRating, int boaPoints, int buaPoints,
			java.time.Instant joinedAt) {
	}

	public record AwardView(String id, UUID studentId, String studentName, String group, String title,
			String detail, String metric, int points, java.time.Instant createdAt) {
	}
}
