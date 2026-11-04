package com.ptu.backend.domain;

public class BattlePassService {

    public static final int MAX_LEVEL = 50;
    public static final int ACTION_POINTS_PER_LEVEL = 20;

    public int calculateLevel(int actionPoints) {
        if (actionPoints < 0) {
            throw new IllegalArgumentException("Action points must be non-negative");
        }
        if (actionPoints == 0) {
            return 0;
        }
        int level = (int) Math.floor((actionPoints - 1) / (double) ACTION_POINTS_PER_LEVEL) + 1;
        return Math.min(level, MAX_LEVEL);
    }

    public int remainingActionPointsToLevel(int actionPoints) {
        if (actionPoints < 0) {
            throw new IllegalArgumentException("Action points must be non-negative");
        }
        if (actionPoints >= MAX_LEVEL * ACTION_POINTS_PER_LEVEL) {
            return 0;
        }
        int currentLevel = calculateLevel(actionPoints);
        int levelStart = (currentLevel - 1) * ACTION_POINTS_PER_LEVEL;
        return ACTION_POINTS_PER_LEVEL - ((actionPoints - levelStart) % ACTION_POINTS_PER_LEVEL);
    }

    public int totalLevelProgress(int actionPoints) {
        return Math.min(actionPoints, MAX_LEVEL * ACTION_POINTS_PER_LEVEL);
    }
}
