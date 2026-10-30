package com.ptu.backend.domain;

import com.ptu.backend.review.ReviewModerationService;
import com.ptu.backend.review.filter.BlockedWordsReviewFilterStrategy;
import com.ptu.backend.review.filter.MinLengthReviewFilterStrategy;
import com.ptu.backend.review.filter.SpamReviewFilterStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
        TeacherBudgetService service = new TeacherBudgetService();
        TeacherBudget budget = new TeacherBudget();
        budget.setTeacherId(7L);
        budget.setTotalBudget(new BigDecimal("150.00"));
        budget.setSpentBudget(BigDecimal.ZERO);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.awardPoints(budget, new BigDecimal("200.00"))
        );

        assertEquals("Teacher budget exceeded", ex.getMessage());
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
