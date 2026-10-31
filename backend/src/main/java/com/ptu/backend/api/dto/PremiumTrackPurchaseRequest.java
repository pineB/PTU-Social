package com.ptu.backend.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PremiumTrackPurchaseRequest(
        UUID studentId,
        Long battlePassSeasonId,
        BigDecimal spentCurrency
) {
}
