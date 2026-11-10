package com.ptu.backend.campus;

import java.util.List;

public final class CampusRules {
    private CampusRules() {
    }

    public static int requiredGroupParticipation(int groupSize, int minimumPercent) {
        if (groupSize < 0 || minimumPercent < 0 || minimumPercent > 100) {
            throw new IllegalArgumentException("Invalid group size or participation threshold");
        }
        return (int) Math.ceil(groupSize * minimumPercent / 100.0d);
    }

    public static int passLevel(int experience) {
        if (experience < 0) {
            throw new IllegalArgumentException("Experience must be non-negative");
        }
        return Math.min(50, (experience + 19) / 20);
    }

    public static int weightedIndex(List<Integer> weights, int ticket) {
        return weightedIndex(weights, (long) ticket);
    }

    public static int weightedIndex(List<Integer> weights, long ticket) {
        if (weights == null || weights.isEmpty() || weights.stream().anyMatch(weight -> weight == null || weight <= 0)) {
            throw new IllegalArgumentException("Weights must be a non-empty list of positive values");
        }
        long total = weights.stream().mapToLong(Integer::longValue).sum();
        if (ticket < 0 || ticket >= total) {
            throw new IllegalArgumentException("Ticket is outside the total weight");
        }
        for (int i = 0; i < weights.size(); i++) {
            ticket -= weights.get(i);
            if (ticket < 0) {
                return i;
            }
        }
        throw new IllegalStateException("Ticket did not select a reward");
    }
}
