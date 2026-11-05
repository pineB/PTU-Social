package com.ptu.backend.campus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CampusRulesTest {
    @Test
    void groupThresholdRoundsUp() {
        assertEquals(0, CampusRules.requiredGroupParticipation(0, 80));
        assertEquals(1, CampusRules.requiredGroupParticipation(1, 1));
        assertEquals(3, CampusRules.requiredGroupParticipation(5, 50));
        assertEquals(5, CampusRules.requiredGroupParticipation(5, 100));
    }

    @Test
    void groupThresholdRejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> CampusRules.requiredGroupParticipation(-1, 50));
        assertThrows(IllegalArgumentException.class, () -> CampusRules.requiredGroupParticipation(5, 101));
    }

    @Test
    void passLevelIsDerivedFromExperience() {
        assertEquals(0, CampusRules.passLevel(0));
        assertEquals(1, CampusRules.passLevel(1));
        assertEquals(1, CampusRules.passLevel(20));
        assertEquals(2, CampusRules.passLevel(21));
        assertEquals(50, CampusRules.passLevel(1000));
        assertThrows(IllegalArgumentException.class, () -> CampusRules.passLevel(-1));
    }

    @Test
    void weightedSelectionHonorsTicketBoundaries() {
        assertEquals(0, CampusRules.weightedIndex(List.of(55, 30, 12, 3), 0));
        assertEquals(0, CampusRules.weightedIndex(List.of(55, 30, 12, 3), 54));
        assertEquals(1, CampusRules.weightedIndex(List.of(55, 30, 12, 3), 55));
        assertEquals(3, CampusRules.weightedIndex(List.of(55, 30, 12, 3), 99));
        assertThrows(IllegalArgumentException.class, () -> CampusRules.weightedIndex(List.of(1, 2), 3));
    }
}
