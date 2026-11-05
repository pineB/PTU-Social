package com.ptu.backend.auth.dto;

import java.time.Instant;

public record TeacherInviteInfoResponse(String fullName, String email, Instant expiresAt) {
}
