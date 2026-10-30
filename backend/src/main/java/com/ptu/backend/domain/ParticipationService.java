package com.ptu.backend.domain;

public class ParticipationService {

    public int calculateRequiredParticipants(int totalParticipants, int minimumPercent) {
        if (totalParticipants < 0) {
            throw new IllegalArgumentException("Total participants must be non-negative");
        }
        if (minimumPercent < 0 || minimumPercent > 100) {
            throw new IllegalArgumentException("Minimum percent must be between 0 and 100");
        }
        return (int) Math.ceil(totalParticipants * minimumPercent / 100.0d);
    }

    public boolean canStartCollectiveActivity(int joinedParticipants, int totalParticipants, int minimumPercent) {
        return joinedParticipants >= calculateRequiredParticipants(totalParticipants, minimumPercent);
    }
}
