package com.ptu.backend.service;

import org.springframework.stereotype.Service;

@Service
public class AcademicRatingService {

    public int calculateAcademicRating(int base, int daysLate) {
        double multiplier = Math.max(0.5, 1 - 0.01 * daysLate);
        return (int) Math.floor(base * multiplier);
    }
}
