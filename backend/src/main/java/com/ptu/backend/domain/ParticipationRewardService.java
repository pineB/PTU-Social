package com.ptu.backend.domain;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipationRewardService {

    private final TeacherBudgetService teacherBudgetService;
    private final ParticipationRepository participationRepository;

    public ParticipationRewardService(TeacherBudgetService teacherBudgetService,
                                      ParticipationRepository participationRepository) {
        this.teacherBudgetService = teacherBudgetService;
        this.participationRepository = participationRepository;
    }

    @Transactional
    public Participation awardParticipation(Activity activity, UUID studentId, int boaPercent, int buaPercent) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity is required");
        }
        if (activity.getId() == null || activity.getTeacherId() == null || studentId == null) {
            throw new IllegalArgumentException("Activity id, teacher id and student id are required");
        }
        if (boaPercent < 0 || boaPercent > 100 || buaPercent < 0 || buaPercent > 100) {
            throw new IllegalArgumentException("BOA/BUA percent must be between 0 and 100");
        }

        if (participationRepository.existsByActivityIdAndStudentId(activity.getId(), studentId)) {
            throw new IllegalStateException("Duplicate reward award for the same student and activity");
        }

        BigDecimal boaAward = teacherBudgetService.calculateAwardAmount(activity.getRewardBoa(), boaPercent);
        BigDecimal buaAward = teacherBudgetService.calculateAwardAmount(activity.getRewardBua(), buaPercent);
        BigDecimal totalAward = boaAward.add(buaAward);

        teacherBudgetService.awardPoints(activity.getTeacherId(), totalAward);

        Participation participation = new Participation();
        participation.setActivityId(activity.getId());
        participation.setStudentId(studentId);
        participation.setPointsBoa(boaAward);
        participation.setPointsBua(buaAward);
        participation.setStatus(ParticipationStatus.ATTENDED);
        return participationRepository.save(participation);
    }
}
