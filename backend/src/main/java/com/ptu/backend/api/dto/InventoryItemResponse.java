package com.ptu.backend.api.dto;

import com.ptu.backend.domain.CaseRarity;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryItemResponse(
        Long id,
        Long caseId,
        UUID ownerStudentId,
        String itemName,
        CaseRarity rarity,
        Integer quantity,
        LocalDateTime acquiredAt
) {
}
