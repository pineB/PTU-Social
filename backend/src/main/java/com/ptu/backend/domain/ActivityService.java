package com.ptu.backend.domain;

import com.ptu.backend.api.dto.ActivityCreateRequest;
import com.ptu.backend.api.dto.ActivityResponse;
import com.ptu.backend.api.dto.JoinActivityRequest;
import com.ptu.backend.api.dto.ParticipationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ParticipationRepository participationRepository;

    public ActivityService(ActivityRepository activityRepository, ParticipationRepository participationRepository) {
        this.activityRepository = activityRepository;
        this.participationRepository = participationRepository;
    }

    @Transactional
    public ActivityResponse createActivity(ActivityCreateRequest request) {
        validateActivityRequest(request);

        Activity activity = new Activity();
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

        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public ParticipationResponse joinActivity(Long activityId, JoinActivityRequest request) {
        if (activityId == null || request == null) {
            throw new IllegalArgumentException("Activity id and request are required");
        }
        if (request.studentId() == null) {
            throw new IllegalArgumentException("Student id is required");
        }

        Activity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
        if (!Boolean.TRUE.equals(activity.getActive())) {
            throw new IllegalArgumentException("Activity is not active");
        }
        if (activity.getType() != ActivityType.COMMUNITY) {
            throw new IllegalArgumentException("Only community activities allow public registration");
        }

        if (participationRepository.existsByActivityIdAndStudentId(activityId, request.studentId())) {
            throw new IllegalArgumentException("Student is already registered for this activity");
        }

        if (participationRepository.countByActivityId(activityId) >= activity.getMaxParticipants()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Activity has reached its participant limit");
        }

        Participation participation = new Participation();
        participation.setActivityId(activityId);
        participation.setStudentId(request.studentId());
        participation.setStatus(ParticipationStatus.REGISTERED);
        participation.setPointsBoa(BigDecimal.ZERO);
        participation.setPointsBua(BigDecimal.ZERO);
        participation = participationRepository.save(participation);

        long registeredCount = participationRepository.countByActivityId(activityId);
        if (activity.getParticipationMode() == ParticipationMode.COLLECTIVE
                && registeredCount >= calculateRequiredParticipants(activity.getMaxParticipants(), activity.getRequiredActivationPercent())) {
            participationRepository.confirmRegisteredByActivityId(
                    activityId,
                    ParticipationStatus.REGISTERED,
                    ParticipationStatus.CONFIRMED
            );
            participation.setStatus(ParticipationStatus.CONFIRMED);
        }

        return toResponse(participation);
    }

    public int calculateRequiredParticipants(int groupSize, int thresholdPercent) {
        if (thresholdPercent < 0 || thresholdPercent > 100) {
            throw new IllegalArgumentException("Threshold percent must be between 0 and 100");
        }
        if (groupSize <= 0) {
            return 0;
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
        if (request.requiredActivationPercent() == null
                || request.requiredActivationPercent() < 0 || request.requiredActivationPercent() > 100) {
            throw new IllegalArgumentException("Required activation percent must be between 0 and 100");
        }
        if (request.maxParticipants() == null || request.maxParticipants() <= 0) {
            throw new IllegalArgumentException("Max participants must be positive");
        }
        if (request.minParticipants() == null || request.minParticipants() < 0
                || request.minParticipants() > request.maxParticipants()) {
            throw new IllegalArgumentException("Min participants cannot exceed max participants");
        }
    }

    private ParticipationResponse toResponse(Participation participation) {
        return new ParticipationResponse(
                participation.getId(),
                participation.getActivityId(),
                participation.getStudentId(),
                participation.getStatus(),
                participation.getPointsBoa(),
                participation.getPointsBua(),
                participation.getCreatedAt(),
                participation.getUpdatedAt()
        );
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
