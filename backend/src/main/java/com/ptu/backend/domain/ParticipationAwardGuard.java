package com.ptu.backend.domain;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class ParticipationAwardGuard {

    private final Set<String> awardedKeys = ConcurrentHashMap.newKeySet();

    public boolean tryRegister(Long activityId, UUID studentId) {
        if (activityId == null || studentId == null) {
            throw new IllegalArgumentException("Activity id and student id are required");
        }
        return awardedKeys.add(activityId + ":" + studentId);
    }
}
