package com.ptu.backend.certificate.web;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.ptu.backend.certificate.domain.CertificateStatus;
import com.ptu.backend.certificate.dto.CertificateRequestView;
import com.ptu.backend.certificate.dto.CreateCertificateRequest;
import com.ptu.backend.certificate.dto.UpdateCertificateStatusRequest;
import com.ptu.backend.certificate.service.CertificateRequestService;
import com.ptu.backend.certificate.service.CertificateRequestService.CertificateDocument;
import com.ptu.backend.common.security.UserPrincipal;

import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

	private static final int MAX_PAGE_SIZE = 100;

	private final CertificateRequestService service;

	public CertificateController(CertificateRequestService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('STUDENT')")
	public CertificateRequestView create(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreateCertificateRequest request) {
		return service.create(principal, request);
	}

	@GetMapping("/my")
	@PreAuthorize("hasRole('STUDENT')")
	public List<CertificateRequestView> listMine(@AuthenticationPrincipal UserPrincipal principal) {
		return service.listMine(principal);
	}

	@GetMapping
	@PreAuthorize("hasRole('ACADEMIC_OFFICE')")
	public Page<CertificateRequestView> list(@RequestParam(required = false) CertificateStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		return service.list(status, PageRequest.of(Math.max(page, 0), safeSize));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('ACADEMIC_OFFICE')")
	public CertificateRequestView updateStatus(@PathVariable UUID id,
			@Valid @RequestBody UpdateCertificateStatusRequest request) {
		return service.updateStatus(id, request);
	}

	@PostMapping(path = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ACADEMIC_OFFICE')")
	public CertificateRequestView uploadDocument(@PathVariable UUID id,
			@RequestParam("file") MultipartFile file) {
		return service.uploadDocument(id, file);
	}

	@GetMapping("/{id}/document")
	public ResponseEntity<byte[]> downloadDocument(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID id) {
		CertificateDocument document = service.document(principal, id);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(document.fileName(), StandardCharsets.UTF_8).build().toString())
				.body(document.data());
	}
}
