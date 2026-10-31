package com.ptu.backend.domain;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

@Service
public class BattlePassRewardService {

    private final Map<Long, Case> cases = new ConcurrentHashMap<>();
    private final Map<UUID, List<InventoryItem>> inventoryByStudent = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> claimedLevelRewardsByStudent = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> unlockedPremiumLevelsByStudent = new ConcurrentHashMap<>();
    private final AtomicLong inventorySequence = new AtomicLong(1L);

    public BattlePassRewardService() {
        Case common = new Case();
        common.setId(1L);
        common.setName("Bronze chest");
        common.setRarity(CaseRarity.COMMON);
        common.setPrice(new BigDecimal("10.00"));
        common.setActive(true);
        cases.put(common.getId(), common);

        Case rare = new Case();
        rare.setId(2L);
        rare.setName("Silver chest");
        rare.setRarity(CaseRarity.RARE);
        rare.setPrice(new BigDecimal("25.00"));
        rare.setActive(true);
        cases.put(rare.getId(), rare);
    }

    public InventoryItem claimLevelReward(BattlePassSeason season, Level level, UUID studentId) {
        if (season == null || level == null || studentId == null) {
            throw new IllegalArgumentException("Season, level and student id are required");
        }
        if (level.getLevelNumber() > season.getCurrentLevel()) {
            throw new IllegalArgumentException("Level reward is not yet unlocked");
        }

        Set<Integer> claimedLevels = claimedLevelRewardsByStudent.computeIfAbsent(studentId, id -> ConcurrentHashMap.newKeySet());
        if (!claimedLevels.add(level.getLevelNumber())) {
            throw new IllegalStateException("Reward for this level has already been granted");
        }

        if (level.getRewardCaseId() == null) {
            return null;
        }

        return openCase(level.getRewardCaseId(), studentId);
    }

    public void purchasePremiumTrack(BattlePassSeason season, UUID studentId, BigDecimal spentCurrency) {
        if (season == null || studentId == null) {
            throw new IllegalArgumentException("Season and student id are required");
        }
        if (spentCurrency == null || spentCurrency.signum() <= 0) {
            throw new IllegalArgumentException("Premium track purchase must cost more than zero");
        }

        season.setPremiumTrackPurchased(true);

        Set<Integer> unlocked = unlockedPremiumLevelsByStudent.computeIfAbsent(studentId, id -> ConcurrentHashMap.newKeySet());
        for (int level = 1; level <= season.getCurrentLevel(); level++) {
            unlocked.add(level);
        }
    }

    public InventoryItem openCase(Long caseId, UUID studentId) {
        if (caseId == null || studentId == null) {
            throw new IllegalArgumentException("Case id and student id are required");
        }

        Case targetCase = cases.get(caseId);
        if (targetCase == null) {
            throw new IllegalArgumentException("Case not found");
        }

        InventoryItem item = new InventoryItem();
        item.setId(inventorySequence.getAndIncrement());
        item.setCaseRef(targetCase);
        item.setOwnerStudentId(studentId);
        item.setItemName(targetCase.getName());
        item.setRarity(targetCase.getRarity());
        item.setQuantity(1);
        item.setAcquiredAt(java.time.LocalDateTime.now());

        inventoryByStudent.computeIfAbsent(studentId, id -> new ArrayList<>()).add(item);
        return item;
    }

    public List<InventoryItem> getInventory(UUID studentId) {
        return inventoryByStudent.getOrDefault(studentId, List.of());
    }
}
