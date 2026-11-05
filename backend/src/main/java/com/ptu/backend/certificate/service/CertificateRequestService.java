package com.ptu.backend.certificate.service;

import java.util.List;
import java.util.UUID;

import com.ptu.backend.certificate.domain.CertificateRequest;
import com.ptu.backend.certificate.domain.CertificateStatus;
import com.ptu.backend.certificate.dto.CertificateRequestView;
import com.ptu.backend.certificate.dto.CreateCertificateRequest;
import com.ptu.backend.certificate.dto.UpdateCertificateStatusRequest;
import com.ptu.backend.certificate.repo.CertificateRequestRepository;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.security.UserPrincipal;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificateRequestService {

	private final CertificateRequestRepository requests;
	private final UserRepository users;

	public CertificateRequestService(CertificateRequestRepository requests, UserRepository users) {
		this.requests = requests;
		this.users = users;
	}

	@Transactional
	public CertificateRequestView create(UserPrincipal principal, CreateCertificateRequest request) {
		CertificateRequest entity = new CertificateRequest(users.getReferenceById(principal.id()), request.type());
		return CertificateRequestView.from(requests.saveAndFlush(entity));
	}

	@Transactional(readOnly = true)
	public List<CertificateRequestView> listMine(UserPrincipal principal) {
		return requests.findByStudentIdOrderByCreatedAtDesc(principal.id()).stream()
				.map(CertificateRequestView::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public Page<CertificateRequestView> list(CertificateStatus status, Pageable pageable) {
		Page<CertificateRequest> page = status == null
				? requests.findAllByOrderByCreatedAtAsc(pageable)
				: requests.findByStatusOrderByCreatedAtAsc(status, pageable);
		return page.map(CertificateRequestView::from);
	}

	@Transactional
	public CertificateRequestView updateStatus(UUID id, UpdateCertificateStatusRequest request) {
		CertificateStatus status = request.status();
		if (status != CertificateStatus.APPROVED && status != CertificateStatus.REJECTED) {
			throw ApiException.badRequest("Допустимы только статусы APPROVED и REJECTED");
		}
		String reason = request.rejectionReason() == null ? null : request.rejectionReason().trim();
		if (status == CertificateStatus.REJECTED && (reason == null || reason.isEmpty())) {
			throw ApiException.badRequest("Причина отклонения обязательна");
		}
		CertificateRequest entity = requests.findWithStudentById(id)
				.orElseThrow(() -> ApiException.notFound("Заявка не найдена"));
		if (entity.getStatus() != CertificateStatus.PENDING) {
			throw ApiException.conflict("Заявка уже обработана");
		}
		entity.setStatus(status);
		entity.setRejectionReason(status == CertificateStatus.REJECTED ? reason : null);
		return CertificateRequestView.from(requests.saveAndFlush(entity));
	}
}
