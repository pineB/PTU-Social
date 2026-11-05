package com.ptu.backend.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record TeacherInviteCreatedResponse(UUID id, String email, Instant expiresAt) {
}
