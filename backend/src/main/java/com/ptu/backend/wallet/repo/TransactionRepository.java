package com.ptu.backend.wallet.repo;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.ptu.backend.wallet.domain.Transaction;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

	Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

	long countByWalletIdAndTypeAndCreatedAtGreaterThanEqual(UUID walletId, String type, Instant from);
}
