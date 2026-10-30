package com.ptu.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AcademicRatingServiceTest {

    private final AcademicRatingService academicRatingService = new AcademicRatingService();

    @Test
    void shouldReturnBaseWhenSubmissionIsOnTime() {
        assertEquals(100, academicRatingService.calculateAcademicRating(100, 0));
    }

    @Test
    void shouldApplyPenaltyUpToFiftyPercent() {
        assertEquals(50, academicRatingService.calculateAcademicRating(100, 100));
    }

    @Test
    void shouldKeepNegativeAcademicRatingNegative() {
        assertEquals(-5, academicRatingService.calculateAcademicRating(-5, 0));
    }

    @Test
    void shouldFloorValueForLateSubmission() {
        assertEquals(32, academicRatingService.calculateAcademicRating(33, 1));
    }
}
