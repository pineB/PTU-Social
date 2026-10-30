package com.ptu.backend.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewardAndBattlePassRulesTest {

    @Test
    void teacherBudgetRespectsBoaAndBuaAwardTotal() {
        TeacherBudget budget = new TeacherBudget();
        budget.setTeacherId(1L);
        budget.setTotalBudget(new BigDecimal("100.00"));
        budget.setSpentBudget(BigDecimal.ZERO);

        TeacherBudgetService service = new TeacherBudgetService();
        BigDecimal award = service.calculateTotalAward(new BigDecimal("50.00"), new BigDecimal("40.00"), 60, 30);

        assertEquals(new BigDecimal("39.00"), award);

        service.awardPoints(budget, award);
        assertEquals(new BigDecimal("39.00"), budget.getSpentBudget());
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
        TeacherBudgetService budgetService = new TeacherBudgetService();
        ParticipationRewardService rewardService = new ParticipationRewardService(budgetService);

        Activity activity = new Activity();
        activity.setId(10L);
        activity.setRewardBoa(new BigDecimal("100.00"));
        activity.setRewardBua(new BigDecimal("50.00"));

        TeacherBudget budget = new TeacherBudget();
        budget.setTeacherId(5L);
        budget.setTotalBudget(new BigDecimal("250.00"));
        budget.setSpentBudget(BigDecimal.ZERO);

        rewardService.awardParticipation(activity, 99L, 50, 50, budget);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> rewardService.awardParticipation(activity, 99L, 50, 50, budget)
        );

        assertEquals("Duplicate reward award for the same student and activity", ex.getMessage());
    }

    @Test
    void levelRewardCaseIsGrantedOnceAndPremiumOpensPastRewards() {
        BattlePassRewardService rewardService = new BattlePassRewardService();

        BattlePassSeason season = new BattlePassSeason();
        season.setId(7L);
        season.setStudentId(42L);
        season.setCurrentLevel(3);
        season.setPremiumTrackPurchased(false);

        Level level = new Level();
        level.setLevelNumber(3);
        level.setRewardCaseId(1L);

        InventoryItem item = rewardService.claimLevelReward(season, level, 42L);
        assertEquals("Bronze chest", item.getItemName());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> rewardService.claimLevelReward(season, level, 42L)
        );
        assertEquals("Reward for this level has already been granted", ex.getMessage());

        rewardService.purchasePremiumTrack(season, 42L, new BigDecimal("40.00"));
        assertEquals(true, season.getPremiumTrackPurchased());
    }
}
