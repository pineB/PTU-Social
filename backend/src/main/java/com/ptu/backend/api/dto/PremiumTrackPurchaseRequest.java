package com.ptu.backend.api.dto;

import java.math.BigDecimal;

public record PremiumTrackPurchaseRequest(
        Long studentId,
        Long battlePassSeasonId,
        BigDecimal spentCurrency
) {
}
