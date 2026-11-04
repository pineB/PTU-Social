package com.ptu.backend.api.dto;

import java.util.UUID;

public record BattlePassResponse(
        UUID studentId,
        String seasonName,
        Integer currentLevel,
        Integer totalActionPoints,
        Integer progressToNextLevel,
        Integer xpToNextLevel
) {
}
