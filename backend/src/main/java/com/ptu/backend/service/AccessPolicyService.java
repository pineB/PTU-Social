package com.ptu.backend.service;

import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AccessPolicyService {

    public enum ActorRole {
        STUDENT,
        TEACHER,
        HEADMAN,
        ACADEMIC_DEPARTMENT
    }

    public enum RouteAccess {
        STUDENT,
        TEACHER,
        HEADMAN,
        ACADEMIC
    }

    public void assertAllowed(ActorRole actorRole, RouteAccess routeAccess) {
        if (actorRole == ActorRole.STUDENT && routeAccess == RouteAccess.TEACHER) {
            throw new IllegalArgumentException("Student cannot access teacher API");
        }
        if (actorRole == ActorRole.TEACHER && routeAccess == RouteAccess.STUDENT) {
            throw new IllegalArgumentException("Teacher cannot access student API");
        }
        if (actorRole == ActorRole.HEADMAN && routeAccess == RouteAccess.TEACHER) {
            throw new IllegalArgumentException("Headman cannot access teacher API");
        }
        if (actorRole == ActorRole.STUDENT && routeAccess == RouteAccess.ACADEMIC) {
            throw new IllegalArgumentException("Student cannot access academic department API");
        }
        if (actorRole == ActorRole.HEADMAN && routeAccess == RouteAccess.ACADEMIC) {
            throw new IllegalArgumentException("Headman cannot access academic department API");
        }
    }

    public void assertHeadmanOwnsGroup(Long headmanStudentId, Long requestedGroupId, Long headmanGroupId) {
        if (headmanStudentId == null || requestedGroupId == null || headmanGroupId == null) {
            throw new IllegalArgumentException("Headman group data is incomplete");
        }
        if (!Objects.equals(requestedGroupId, headmanGroupId)) {
            throw new IllegalArgumentException("Headman can confirm only his own group");
        }
    }
}
