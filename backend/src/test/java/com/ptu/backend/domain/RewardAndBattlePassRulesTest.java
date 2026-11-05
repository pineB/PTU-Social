package com.ptu.backend.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardAndBattlePassRulesTest {

    @Test
    void teacherBudgetRespectsBoaAndBuaAwardTotal() {
        UUID teacherId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        TeacherBudget budget = new TeacherBudget();
        budget.setTeacherId(teacherId);
        budget.setTotalBudget(new BigDecimal("100.00"));
        budget.setSpentBudget(BigDecimal.ZERO);
        TeacherBudgetRepository repository = mock(TeacherBudgetRepository.class);
        when(repository.addSpentBudgetIfAvailable(teacherId, new BigDecimal("42.00"))).thenReturn(1);
        when(repository.findByTeacherId(teacherId)).thenReturn(Optional.of(budget));

        TeacherBudgetService service = new TeacherBudgetService(repository);
        BigDecimal award = service.calculateTotalAward(new BigDecimal("50.00"), new BigDecimal("40.00"), 60, 30);

        assertEquals(new BigDecimal("42.00"), award);

        service.awardPoints(teacherId, award);
        verify(repository).addSpentBudgetIfAvailable(teacherId, award);
    }

    @Test
    void battlePass50LevelsCap() {
        BattlePassService service = new BattlePassService();

        assertEquals(0, service.calculateLevel(0));
        assertEquals(1, service.calculateLevel(19));
        assertEquals(1, service.calculateLevel(20));
        assertEquals(2, service.calculateLevel(40));
        assertEquals(50, service.calculateLevel(1000));
    }

    @Test
    void duplicateRewardAwardIsRejected() {
        TeacherBudgetRepository budgetRepository = mock(TeacherBudgetRepository.class);
        TeacherBudgetService budgetService = new TeacherBudgetService(budgetRepository);
        ParticipationRepository participationRepository = mock(ParticipationRepository.class);
        ParticipationRewardService rewardService = new ParticipationRewardService(budgetService, participationRepository);

        Activity activity = new Activity();
        activity.setId(10L);
        activity.setTeacherId(UUID.fromString("00000000-0000-0000-0000-000000000005"));
        activity.setRewardBoa(new BigDecimal("100.00"));
        activity.setRewardBua(new BigDecimal("50.00"));

        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        when(participationRepository.existsByActivityIdAndStudentId(10L, studentId)).thenReturn(true);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> rewardService.awardParticipation(activity, studentId, 50, 50)
        );

        assertEquals("Duplicate reward award for the same student and activity", ex.getMessage());
    }
}
