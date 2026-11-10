package com.ptu.backend.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RewardAndBattlePassRulesTest {

    @Test
    void participationAwardsBoaAndBuaWithoutSpendingLimits() {
        ParticipationRepository repository = mock(ParticipationRepository.class);
        ParticipationRewardService service = new ParticipationRewardService(repository);
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setTeacherId(UUID.fromString("00000000-0000-0000-0000-000000000005"));
        activity.setRewardBoa(new BigDecimal("50.00"));
        activity.setRewardBua(new BigDecimal("40.00"));
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        when(repository.existsByActivityIdAndStudentId(10L, studentId)).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.any(Participation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Participation awarded = service.awardParticipation(activity, studentId, 60, 30);

        assertEquals(new BigDecimal("30.00"), awarded.getPointsBoa());
        assertEquals(new BigDecimal("12.00"), awarded.getPointsBua());
        assertEquals(ParticipationStatus.ATTENDED, awarded.getStatus());
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
        ParticipationRepository participationRepository = mock(ParticipationRepository.class);
        ParticipationRewardService rewardService = new ParticipationRewardService(participationRepository);

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
