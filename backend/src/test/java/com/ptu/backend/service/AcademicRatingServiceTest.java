package com.ptu.backend.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AcademicRatingServiceTest {

    private final AcademicRatingService academicRatingService = new AcademicRatingService();

    @Test
    void shouldReturnBaseWhenSubmissionIsOnTime() {
        assertAll(
            () -> assertEquals(100, academicRatingService.calculateAcademicRating(100, 0)),
            () -> assertEquals(0, academicRatingService.calculateAcademicRating(0, 0)),
            () -> assertEquals(0, academicRatingService.calculateAcademicRating(-5, 0))
        );
    }

    @Test
    void shouldApplyPenaltyUpToFiftyPercent() {
        assertAll(
            () -> assertEquals(100, academicRatingService.calculateAcademicRating(100, 0)),
            () -> assertEquals(99, academicRatingService.calculateAcademicRating(100, 1)),
            () -> assertEquals(90, academicRatingService.calculateAcademicRating(100, 10)),
            () -> assertEquals(50, academicRatingService.calculateAcademicRating(100, 100)),
            () -> assertEquals(50, academicRatingService.calculateAcademicRating(100, 150))
        );
    }

    @Test
    void shouldNeverGoBelowZero() {
        assertEquals(0, academicRatingService.calculateAcademicRating(-10, 0));
        assertEquals(5, academicRatingService.calculateAcademicRating(10, 1000));
    }

    @Test
    void shouldFloorValueForLateSubmission() {
        assertEquals(32, academicRatingService.calculateAcademicRating(33, 1));
    }
}
