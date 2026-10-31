package com.ptu.backend.api;

import com.ptu.backend.api.dto.ActivityCreateRequest;
import com.ptu.backend.api.dto.JoinActivityRequest;
import com.ptu.backend.api.dto.ReviewCreateRequest;
import com.ptu.backend.domain.ActivityService;
import com.ptu.backend.domain.ActivityType;
import com.ptu.backend.domain.ParticipationMode;
import com.ptu.backend.review.ReviewService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityReviewApiRulesTest {

    @Test
    void collectiveActivityUsesCeilThreshold() {
        ActivityService service = new ActivityService();

        assertEquals(5, service.calculateRequiredParticipants(11, 45));
        assertEquals(10, service.calculateRequiredParticipants(20, 50));
    }

    @Test
    void publicActivityRegistrationRequiresThreshold() {
        ActivityService service = new ActivityService();
        ActivityCreateRequest request = new ActivityCreateRequest(
                "Open community event",
                "Community event",
                ActivityType.COMMUNITY,
                ParticipationMode.COLLECTIVE,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                11,
                3,
                45,
                UUID.fromString("00000000-0000-0000-0000-000000000007"),
                BigDecimal.TEN,
                BigDecimal.ONE
        );

        service.createActivity(request);
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var response = service.joinActivity(1L, new JoinActivityRequest(studentId, 1L, 1));

        assertEquals(studentId, response.studentId());
    }

    @Test
    void reviewLengthAndSpamFiltersAreApplied() {
        ReviewService service = new ReviewService();

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
