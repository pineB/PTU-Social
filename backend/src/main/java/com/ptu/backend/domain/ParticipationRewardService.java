package com.ptu.backend.domain;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ParticipationRewardService {

    private final TeacherBudgetService teacherBudgetService;
    private final Set<String> awardedParticipationKeys = ConcurrentHashMap.newKeySet();

    public ParticipationRewardService(TeacherBudgetService teacherBudgetService) {
        this.teacherBudgetService = teacherBudgetService;
    }

    public Participation awardParticipation(Activity activity, Long studentId, int boaPercent, int buaPercent, TeacherBudget budget) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity is required");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("Student id is required");
        }
        if (boaPercent < 0 || boaPercent > 100 || buaPercent < 0 || buaPercent > 100) {
            throw new IllegalArgumentException("BOA/BUA percent must be between 0 and 100");
        }

        String awardKey = activity.getId() + ":" + studentId;
        if (!awardedParticipationKeys.add(awardKey)) {
            throw new IllegalStateException("Duplicate reward award for the same student and activity");
        }

        BigDecimal boaAward = teacherBudgetService.calculateAwardAmount(activity.getRewardBoa(), boaPercent);
        BigDecimal buaAward = teacherBudgetService.calculateAwardAmount(activity.getRewardBua(), buaPercent);
        BigDecimal totalAward = boaAward.add(buaAward);

        teacherBudgetService.awardPoints(budget, totalAward);

        Participation participation = new Participation();
        participation.setActivityId(activity.getId());
        participation.setStudentId(studentId);
        participation.setPointsBoa(boaAward);
        participation.setPointsBua(buaAward);
        participation.setStatus(ParticipationStatus.ATTENDED);
        return participation;
    }
}
