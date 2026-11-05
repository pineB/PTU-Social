package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.wallet.domain.Transaction;
import com.ptu.backend.wallet.repo.TransactionRepository;
import com.ptu.backend.wallet.service.WalletService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WalletServiceIT extends AbstractIntegrationTest {

	@Autowired
	private WalletService walletService;

	@Autowired
	private TransactionRepository transactions;

	@Test
	void creditIsIdempotentByKey() {
		UUID userId = student().getId();

		Transaction first = walletService.credit(userId, new BigDecimal("5"),
				WalletService.TYPE_REVIEW_REWARD, "review-reward:1");
		Transaction second = walletService.credit(userId, new BigDecimal("5"),
				WalletService.TYPE_REVIEW_REWARD, "review-reward:1");

		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(walletService.balance(userId)).isEqualByComparingTo("5");
		assertThat(transactions.count()).isEqualTo(1);
	}

	@Test
	void creditsWithDifferentKeysAccumulate() {
		UUID userId = student().getId();

		walletService.credit(userId, new BigDecimal("5"), WalletService.TYPE_REVIEW_REWARD, "k-1");
		walletService.credit(userId, new BigDecimal("2.50"), WalletService.TYPE_ADJUSTMENT, "k-2");

		assertThat(walletService.balance(userId)).isEqualByComparingTo("7.50");
	}

	@Test
	void walletIsCreatedLazily() {
		User user = student();

		assertThat(walletService.balance(user.getId())).isEqualByComparingTo("0");

		walletService.credit(user.getId(), BigDecimal.ONE, WalletService.TYPE_ADJUSTMENT, "lazy-1");

		assertThat(walletService.balance(user.getId())).isEqualByComparingTo("1");
	}

	@Test
	void nonPositiveAmountIsRejected() {
		UUID userId = student().getId();

		assertThatThrownBy(() -> walletService.credit(userId, BigDecimal.ZERO,
				WalletService.TYPE_REVIEW_REWARD, "zero"))
				.isInstanceOf(ApiException.class)
				.hasMessage("Сумма должна быть положительной");
		assertThat(walletService.balance(userId)).isEqualByComparingTo("0");
	}

	private User student() {
		return createUser(uniqueEmail("wallet"), "Password123", Role.STUDENT);
	}
}
