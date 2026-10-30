package com.ptu.backend.auth.domain;

public enum RegistrationStatus {
	PENDING_EMAIL,
	EMAIL_VERIFIED,
	APPROVED,
	REJECTED,
	EXPIRED
}
