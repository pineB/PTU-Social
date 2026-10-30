package com.ptu.backend.api.dto;

import com.ptu.backend.domain.CaseRarity;

import java.time.LocalDateTime;

public record InventoryItemResponse(
        Long id,
        Long caseId,
        Long ownerStudentId,
        String itemName,
        CaseRarity rarity,
        Integer quantity,
        LocalDateTime acquiredAt
) {
}
