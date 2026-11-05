package com.ptu.backend.certificate.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.ptu.backend.certificate.domain.CertificateRequest;
import com.ptu.backend.certificate.domain.CertificateRequestDocument;
import com.ptu.backend.certificate.domain.CertificateStatus;
import com.ptu.backend.certificate.dto.CertificateRequestView;
import com.ptu.backend.certificate.dto.CreateCertificateRequest;
import com.ptu.backend.certificate.dto.UpdateCertificateStatusRequest;
import com.ptu.backend.certificate.repo.CertificateRequestRepository;
import com.ptu.backend.certificate.repo.CertificateRequestDocumentRepository;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.security.UserPrincipal;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CertificateRequestService {

	private static final long MAX_DOCUMENT_SIZE = 20L * 1024 * 1024;
	private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

	private final CertificateRequestRepository requests;
	private final CertificateRequestDocumentRepository documents;
	private final UserRepository users;

	public CertificateRequestService(CertificateRequestRepository requests,
			CertificateRequestDocumentRepository documents, UserRepository users) {
		this.requests = requests;
		this.documents = documents;
		this.users = users;
	}

	@Transactional
	public CertificateRequestView create(UserPrincipal principal, CreateCertificateRequest request) {
		CertificateRequest entity = new CertificateRequest(users.getReferenceById(principal.id()), request.type());
		return CertificateRequestView.from(requests.saveAndFlush(entity), null);
	}

	@Transactional(readOnly = true)
	public List<CertificateRequestView> listMine(UserPrincipal principal) {
		return requests.findByStudentIdOrderByCreatedAtDesc(principal.id()).stream()
				.map(request -> toView(request))
				.toList();
	}

	@Transactional(readOnly = true)
	public Page<CertificateRequestView> list(CertificateStatus status, Pageable pageable) {
		Page<CertificateRequest> page = status == null
				? requests.findAllByOrderByCreatedAtAsc(pageable)
				: requests.findByStatusOrderByCreatedAtAsc(status, pageable);
		return page.map(this::toView);
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
		if (status == CertificateStatus.REJECTED) {
			documents.deleteById(id);
		}
		return CertificateRequestView.from(requests.saveAndFlush(entity), null);
	}

	@Transactional
	public CertificateRequestView uploadDocument(UUID id, MultipartFile file) {
		CertificateRequest entity = requests.findWithStudentById(id)
				.orElseThrow(() -> ApiException.notFound("Заявка не найдена"));
		if (entity.getStatus() == CertificateStatus.REJECTED) {
			throw ApiException.conflict("К отклонённой заявке нельзя прикрепить справку");
		}
		if (file == null || file.isEmpty()) {
			throw ApiException.badRequest("Выберите PDF-файл справки");
		}
		if (file.getSize() > MAX_DOCUMENT_SIZE) {
			throw ApiException.badRequest("Размер PDF-файла не должен превышать 20 МБ");
		}
		String fileName = file.getOriginalFilename();
		if (fileName == null || fileName.isBlank() || fileName.length() > 255 || fileName.contains("/")
				|| fileName.contains("\\") || fileName.contains("\r") || fileName.contains("\n")
				|| !fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			throw ApiException.badRequest("Имя файла должно иметь расширение .pdf");
		}
		try {
			byte[] data = file.getBytes();
			if (data.length < PDF_SIGNATURE.length || !hasPdfSignature(data)) {
				throw ApiException.badRequest("Файл не является PDF-документом");
			}
			documents.saveAndFlush(new CertificateRequestDocument(id, fileName, data));
		} catch (IOException exception) {
			throw ApiException.badRequest("Не удалось прочитать PDF-файл");
		}
		return toView(entity);
	}

	@Transactional(readOnly = true)
	public CertificateDocument document(UserPrincipal principal, UUID id) {
		if (principal == null) {
			throw ApiException.unauthorized("Требуется авторизация");
		}
		CertificateRequest entity = requests.findWithStudentById(id)
				.orElseThrow(() -> ApiException.notFound("Заявка не найдена"));
		if (principal.role() != Role.ACADEMIC_OFFICE) {
			if (!entity.getStudent().getId().equals(principal.id()) || entity.getStatus() != CertificateStatus.APPROVED) {
				throw ApiException.forbidden("Нет доступа к файлу справки");
			}
		}
		CertificateRequestDocument document = documents.findById(id)
				.orElseThrow(() -> ApiException.notFound("Файл справки ещё не загружен"));
		return new CertificateDocument(document.getDocumentName(), document.getDocumentData());
	}

	private CertificateRequestView toView(CertificateRequest request) {
		String documentName = documents.findDocumentNameByRequestId(request.getId()).orElse(null);
		return CertificateRequestView.from(request, documentName);
	}

	private boolean hasPdfSignature(byte[] data) {
		for (int index = 0; index < PDF_SIGNATURE.length; index++) {
			if (data[index] != PDF_SIGNATURE[index]) return false;
		}
		return true;
	}

	public record CertificateDocument(String fileName, byte[] data) {
	}
}
