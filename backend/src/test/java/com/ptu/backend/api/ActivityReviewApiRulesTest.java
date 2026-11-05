package com.ptu.backend.api;

import com.ptu.backend.api.dto.ActivityCreateRequest;
import com.ptu.backend.api.dto.JoinActivityRequest;
import com.ptu.backend.api.dto.ReviewCreateRequest;
import com.ptu.backend.domain.ActivityService;
import com.ptu.backend.domain.ActivityRepository;
import com.ptu.backend.domain.ParticipationRepository;
import com.ptu.backend.domain.ActivityType;
import com.ptu.backend.domain.Activity;
import com.ptu.backend.domain.ParticipationMode;
import com.ptu.backend.review.ReviewService;
import com.ptu.backend.review.ReviewRepository;
import com.ptu.backend.review.ReviewCurrencyAwarder;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityReviewApiRulesTest {

    @Test
    void collectiveActivityUsesCeilThreshold() {
        ActivityService service = new ActivityService(
            mock(ActivityRepository.class),
            mock(ParticipationRepository.class)
        );

        assertEquals(5, service.calculateRequiredParticipants(11, 45));
        assertEquals(10, service.calculateRequiredParticipants(20, 50));
    }

    @Test
    void publicActivityRegistrationRequiresThreshold() {
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        ParticipationRepository participationRepository = mock(ParticipationRepository.class);
        ActivityService service = new ActivityService(activityRepository, participationRepository);
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setType(ActivityType.COMMUNITY);
        activity.setParticipationMode(ParticipationMode.COLLECTIVE);
        activity.setActive(true);
        activity.setMaxParticipants(11);
        activity.setRequiredActivationPercent(45);
        when(activityRepository.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(activity));
        when(participationRepository.countByActivityId(1L)).thenReturn(0L, 1L);
        when(participationRepository.save(org.mockito.ArgumentMatchers.any()))
            .thenAnswer(invocation -> {
                var participation = invocation.<com.ptu.backend.domain.Participation>getArgument(0);
                participation.setId(8L);
                participation.setCreatedAt(LocalDateTime.now());
                participation.setUpdatedAt(LocalDateTime.now());
                return participation;
            });
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var response = service.joinActivity(1L, new JoinActivityRequest(studentId, 1L, 1));

        assertEquals(studentId, response.studentId());
        assertEquals(0, response.pointsBoa().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.pointsBua().compareTo(BigDecimal.ZERO));
    }

    @Test
    void reviewLengthAndSpamFiltersAreApplied() {
        ReviewService service = new ReviewService(
            mock(ReviewRepository.class),
            mock(ObjectProvider.class)
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createReview(new ReviewCreateRequest(
                    1L,
                    UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    5,
                    "short text"
                ))
        );

        assertEquals("Review rejected by moderation filters", ex.getMessage());
    }
}
