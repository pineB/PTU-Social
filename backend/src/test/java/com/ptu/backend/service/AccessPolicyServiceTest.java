package com.ptu.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AccessPolicyServiceTest {

    private final AccessPolicyService accessPolicyService = new AccessPolicyService();

    @Test
    void shouldAllowStudentToUseStudentApi() {
        assertDoesNotThrow(() -> accessPolicyService.assertAllowed(AccessPolicyService.ActorRole.STUDENT, AccessPolicyService.RouteAccess.STUDENT));
    }

    @Test
    void shouldRejectStudentAttemptToUseTeacherApi() {
        assertThrows(
            IllegalArgumentException.class,
            () -> accessPolicyService.assertAllowed(AccessPolicyService.ActorRole.STUDENT, AccessPolicyService.RouteAccess.TEACHER)
        );
    }

    @Test
    void shouldAllowHeadmanToConfirmOnlyHisOwnGroup() {
        assertDoesNotThrow(() -> accessPolicyService.assertHeadmanOwnsGroup(10L, 20L, 20L));
    }

    @Test
    void shouldRejectHeadmanConfirmationForForeignGroup() {
        assertThrows(
            IllegalArgumentException.class,
            () -> accessPolicyService.assertHeadmanOwnsGroup(10L, 20L, 99L)
        );
    }
}
