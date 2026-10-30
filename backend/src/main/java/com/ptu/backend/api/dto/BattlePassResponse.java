package com.ptu.backend.api.dto;

public record BattlePassResponse(
        Long studentId,
        String seasonName,
        Integer currentLevel,
        Integer totalActionPoints,
        Integer progressToNextLevel,
        Integer xpToNextLevel
) {
}
