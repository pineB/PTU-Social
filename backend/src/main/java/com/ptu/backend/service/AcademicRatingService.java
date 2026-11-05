package com.ptu.backend.service;

import org.springframework.stereotype.Service;

@Service
public class AcademicRatingService {

    public int calculateAcademicRating(int base, int daysLate) {
        int safeBase = Math.max(0, base);
        int safeLateDays = Math.max(0, daysLate);
        double multiplier = Math.max(0.5, 1 - 0.01 * safeLateDays);
        return Math.max(0, (int) Math.floor(safeBase * multiplier));
    }
}
