package com.ptu.backend.user.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.ptu.backend.user.dto.ManagedUserView;
import com.ptu.backend.user.service.AdminTestGrantService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/test-grants")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTestGrantController {

	private final AdminTestGrantService service;

	public AdminTestGrantController(AdminTestGrantService service) {
		this.service = service;
	}

	@GetMapping("/students")
	public List<ManagedUserView> students() {
		return service.students();
	}

	@PostMapping("/currency")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void grantCurrency(@Valid @RequestBody GrantCurrencyRequest request) {
		service.grantCurrency(request.studentId(), request.amount());
	}

	@PostMapping("/items")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void grantItem(@Valid @RequestBody GrantItemRequest request) {
		service.grantItem(request.studentId(), request.rewardId());
	}

	public record GrantCurrencyRequest(@NotNull UUID studentId, @NotNull @Positive @Digits(integer = 17, fraction = 2)
			BigDecimal amount) {
	}

	public record GrantItemRequest(@NotNull UUID studentId, @NotNull @Positive Long rewardId) {
	}
}
