package com.ptu.backend.domain;

import com.ptu.backend.review.ReviewModerationService;
import com.ptu.backend.review.filter.BlockedWordsReviewFilterStrategy;
import com.ptu.backend.review.filter.MinLengthReviewFilterStrategy;
import com.ptu.backend.review.filter.SpamReviewFilterStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DomainBusinessRulesTest {

    @Test
    void collectiveRegistrationThresholdUsesCeil() {
        ParticipationService service = new ParticipationService();

        assertEquals(5, service.calculateRequiredParticipants(11, 45));
        assertEquals(10, service.calculateRequiredParticipants(20, 50));
    }

    @Test
    void teacherBudgetCannotBeExceeded() {
        UUID teacherId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        TeacherBudgetRepository repository = mock(TeacherBudgetRepository.class);
        when(repository.addSpentBudgetIfAvailable(teacherId, new BigDecimal("200.00"))).thenReturn(0);
        when(repository.existsByTeacherId(teacherId)).thenReturn(true);
        TeacherBudgetService service = new TeacherBudgetService(repository);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.awardPoints(teacherId, new BigDecimal("200.00"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void spamAndBlockedWordsAreRejected() {
        ReviewModerationService service = new ReviewModerationService(
                java.util.List.of(
                        new MinLengthReviewFilterStrategy(20),
                        new BlockedWordsReviewFilterStrategy(java.util.List.of("buy now", "viagra", "promo")),
                        new SpamReviewFilterStrategy()
                )
        );

        assertFalse(service.isApproved("buy now cheap viagra!!!"));
    }

    @Test
    void battlePassLevelsAreCalculatedFromTwentyAp() {
        BattlePassService service = new BattlePassService();

        assertEquals(1, service.calculateLevel(19));
        assertEquals(1, service.calculateLevel(20));
        assertEquals(2, service.calculateLevel(40));
    }
}
