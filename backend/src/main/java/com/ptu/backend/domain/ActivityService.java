package com.ptu.backend.domain;

import com.ptu.backend.api.dto.ActivityCreateRequest;
import com.ptu.backend.api.dto.ActivityResponse;
import com.ptu.backend.api.dto.JoinActivityRequest;
import com.ptu.backend.api.dto.ParticipationResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ActivityService {

    private final Map<Long, Activity> activities = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> registeredStudentsByActivity = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    public ActivityResponse createActivity(ActivityCreateRequest request) {
        validateActivityRequest(request);

        Activity activity = new Activity();
        activity.setId(sequence.getAndIncrement());
        activity.setTitle(request.title());
        activity.setDescription(request.description());
        activity.setType(request.type());
        activity.setParticipationMode(request.participationMode());
        activity.setStartsAt(request.startsAt());
        activity.setEndsAt(request.endsAt());
        activity.setMaxParticipants(request.maxParticipants());
        activity.setMinParticipants(request.minParticipants());
        activity.setRequiredActivationPercent(request.requiredActivationPercent());
        activity.setTeacherId(request.teacherId());
        activity.setRewardBoa(request.rewardBoa() == null ? BigDecimal.ZERO : request.rewardBoa());
        activity.setRewardBua(request.rewardBua() == null ? BigDecimal.ZERO : request.rewardBua());
        activity.setActive(true);
        activity.setCreatedAt(LocalDateTime.now());

        activities.put(activity.getId(), activity);
        registeredStudentsByActivity.computeIfAbsent(activity.getId(), key -> ConcurrentHashMap.newKeySet());

        return toResponse(activity);
    }

    public ParticipationResponse joinActivity(Long activityId, JoinActivityRequest request) {
        if (activityId == null || request == null) {
            throw new IllegalArgumentException("Activity id and request are required");
        }
        if (request.studentId() == null) {
            throw new IllegalArgumentException("Student id is required");
        }

        Activity activity = activities.get(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("Activity not found");
        }
        if (!Boolean.TRUE.equals(activity.getActive())) {
            throw new IllegalArgumentException("Activity is not active");
        }

        Set<Long> registeredStudents = registeredStudentsByActivity.computeIfAbsent(activityId, key -> ConcurrentHashMap.newKeySet());
        if (!registeredStudents.add(request.studentId())) {
            throw new IllegalArgumentException("Student is already registered for this activity");
        }

        int registeredCount = registeredStudents.size();
        if (activity.getParticipationMode() == ParticipationMode.COLLECTIVE) {
            int requiredParticipants = calculateRequiredParticipants(activity.getMaxParticipants(), activity.getRequiredActivationPercent());
            if (registeredCount < requiredParticipants) {
                return new ParticipationResponse(
                        null,
                        activity.getId(),
                        request.studentId(),
                        ParticipationStatus.REGISTERED,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
            }
        }

        Participation participation = new Participation();
        participation.setActivityId(activityId);
        participation.setStudentId(request.studentId());
        participation.setStatus(ParticipationStatus.REGISTERED);
        participation.setPointsBoa(activity.getRewardBoa());
        participation.setPointsBua(activity.getRewardBua());

        return new ParticipationResponse(
                1L,
                activity.getId(),
                request.studentId(),
                participation.getStatus(),
                participation.getPointsBoa(),
                participation.getPointsBua(),
                participation.getCreatedAt(),
                participation.getUpdatedAt()
        );
    }

    public int calculateRequiredParticipants(int groupSize, int thresholdPercent) {
        if (groupSize <= 0) {
            return 0;
        }
        if (thresholdPercent < 0 || thresholdPercent > 100) {
            throw new IllegalArgumentException("Threshold percent must be between 0 and 100");
        }
        return (int) Math.ceil(groupSize * thresholdPercent / 100.0d);
    }

    private void validateActivityRequest(ActivityCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (request.startsAt() == null || request.endsAt() == null) {
            throw new IllegalArgumentException("Activity start and end time are required");
        }
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("Activity end time must be after start time");
        }
        if (request.requiredActivationPercent() < 0 || request.requiredActivationPercent() > 100) {
            throw new IllegalArgumentException("Required activation percent must be between 0 and 100");
        }
        if (request.maxParticipants() <= 0) {
            throw new IllegalArgumentException("Max participants must be positive");
        }
        if (request.minParticipants() > request.maxParticipants()) {
            throw new IllegalArgumentException("Min participants cannot exceed max participants");
        }
    }

    private ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getType(),
                activity.getParticipationMode(),
                activity.getStartsAt(),
                activity.getEndsAt(),
                activity.getMaxParticipants(),
                activity.getMinParticipants(),
                activity.getRequiredActivationPercent(),
                activity.getRewardBoa(),
                activity.getRewardBua(),
                activity.getTeacherId(),
                activity.getActive(),
                activity.getCreatedAt()
        );
    }
}
