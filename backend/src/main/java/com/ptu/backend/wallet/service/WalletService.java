package com.ptu.backend.wallet.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.wallet.domain.Transaction;
import com.ptu.backend.wallet.domain.Wallet;
import com.ptu.backend.wallet.repo.TransactionRepository;
import com.ptu.backend.wallet.repo.WalletRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WalletService {

	public static final String TYPE_REVIEW_REWARD = "REVIEW_REWARD";
	public static final String TYPE_MONTHLY_CONVERSION = "MONTHLY_CONVERSION";
	public static final String TYPE_ADJUSTMENT = "ADJUSTMENT";

	private final WalletRepository wallets;
	private final TransactionRepository transactions;

	public WalletService(WalletRepository wallets, TransactionRepository transactions) {
		this.wallets = wallets;
		this.transactions = transactions;
	}

	/**
	 * Credits the user's wallet. Repeated calls with the same {@code idempotencyKey}
	 * return the existing transaction and leave the balance untouched.
	 */
	@Transactional
	public Transaction credit(UUID userId, BigDecimal amount, String type, String idempotencyKey) {
		if (userId == null) {
			throw ApiException.badRequest("Пользователь обязателен");
		}
		if (amount == null || amount.signum() <= 0) {
			throw ApiException.badRequest("Сумма должна быть положительной");
		}
		if (type == null || type.isBlank()) {
			throw ApiException.badRequest("Тип транзакции обязателен");
		}
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw ApiException.badRequest("Ключ идемпотентности обязателен");
		}

		Optional<Transaction> existing = transactions.findByIdempotencyKey(idempotencyKey);
		if (existing.isPresent()) {
			log.debug("Duplicate credit ignored, idempotencyKey={}", idempotencyKey);
			return existing.get();
		}

		Wallet wallet = lockOrCreate(userId);
		wallet.setCurrencyBalance(wallet.getCurrencyBalance().add(amount));

		try {
			return transactions.saveAndFlush(
					new Transaction(wallet.getId(), amount, type, idempotencyKey));
		}
		catch (DataIntegrityViolationException ex) {
			throw ApiException.conflict("Транзакция с таким ключом идемпотентности уже существует");
		}
	}

	@Transactional(readOnly = true)
	public BigDecimal balance(UUID userId) {
		return wallets.findByUserId(userId)
				.map(Wallet::getCurrencyBalance)
				.orElse(BigDecimal.ZERO);
	}

	@Transactional(readOnly = true)
	public long countCreditsSince(UUID userId, String type, Instant from) {
		return wallets.findByUserId(userId)
				.map(wallet -> transactions.countByWalletIdAndTypeAndCreatedAtGreaterThanEqual(
						wallet.getId(), type, from))
				.orElse(0L);
	}

	private Wallet lockOrCreate(UUID userId) {
		wallets.insertIfAbsent(userId);
		return wallets.findByUserIdForUpdate(userId)
				.orElseThrow(() -> ApiException.notFound("Кошелёк не найден"));
	}
}
