package com.ptu.backend.auth.service;

import java.time.Instant;
import java.util.List;

import com.ptu.backend.auth.domain.RegistrationRequest;
import com.ptu.backend.auth.domain.RegistrationStatus;
import com.ptu.backend.auth.domain.TeacherInvite;
import com.ptu.backend.auth.domain.TeacherInviteStatus;
import com.ptu.backend.auth.repo.RegistrationRequestRepository;
import com.ptu.backend.auth.repo.TeacherInviteRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExpirationScheduler {

	private final RegistrationRequestRepository requests;
	private final TeacherInviteRepository invites;
	private final EmailVerificationService emailVerification;

	public ExpirationScheduler(RegistrationRequestRepository requests, TeacherInviteRepository invites,
			EmailVerificationService emailVerification) {
		this.requests = requests;
		this.invites = invites;
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

		List<TeacherInvite> expiredInvites = invites.findByStatusAndExpiresAtBefore(TeacherInviteStatus.PENDING, now);
		for (TeacherInvite invite : expiredInvites) {
			invite.setStatus(TeacherInviteStatus.EXPIRED);
		}
		if (!expiredInvites.isEmpty()) {
			log.info("Expired {} teacher invites", expiredInvites.size());
		}
	}
}
