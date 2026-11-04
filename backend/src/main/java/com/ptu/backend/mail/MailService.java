package com.ptu.backend.mail;

import java.time.Duration;

public interface MailService {

	void sendVerificationCode(String email, String fullName, String code, Duration ttl);

	void sendTeacherInvite(String email, String fullName, String inviteLink, Duration ttl);
}
