package com.ptu.backend.domain;

public class BattlePassService {

    public int calculateLevel(int actionPoints) {
        if (actionPoints < 0) {
            throw new IllegalArgumentException("Action points must be non-negative");
        }
        if (actionPoints == 0) {
            return 0;
        }
        return (int) Math.ceil(actionPoints / 20.0d);
    }

    public int remainingActionPointsToLevel(int actionPoints) {
        int level = calculateLevel(actionPoints);
        return 20 - (actionPoints % 20);
    }
}
