package com.ptu.backend.domain;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ParticipationAwardGuard {

    private final Set<String> awardedKeys = ConcurrentHashMap.newKeySet();

    public boolean tryRegister(String activityId, Long studentId) {
        if (activityId == null || studentId == null) {
            throw new IllegalArgumentException("Activity id and student id are required");
        }
        return awardedKeys.add(activityId + ":" + studentId);
    }
}
