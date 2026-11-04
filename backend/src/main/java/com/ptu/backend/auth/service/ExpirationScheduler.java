package com.ptu.backend.auth.service;

import java.time.Instant;
import java.util.List;

import com.ptu.backend.auth.domain.RegistrationRequest;
import com.ptu.backend.auth.domain.RegistrationStatus;
import com.ptu.backend.auth.repo.RegistrationRequestRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExpirationScheduler {

	private final RegistrationRequestRepository requests;
	private final EmailVerificationService emailVerification;

	public ExpirationScheduler(RegistrationRequestRepository requests, EmailVerificationService emailVerification) {
		this.requests = requests;
		this.emailVerification = emailVerification;
	}

	@Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
	@Transactional
	public void expireOutdated() {
		Instant now = Instant.now();

		List<RegistrationRequest> expiredRequests = requests.findByStatusInAndExpiresAtBefore(
				List.of(RegistrationStatus.PENDING_EMAIL, RegistrationStatus.EMAIL_VERIFIED), now);
		for (RegistrationRequest request : expiredRequests) {
			request.setStatus(RegistrationStatus.EXPIRED);
			request.setPasswordHash(null);
			emailVerification.invalidate(request.getId());
		}
		if (!expiredRequests.isEmpty()) {
			log.info("Expired {} registration requests", expiredRequests.size());
		}

	}
}
