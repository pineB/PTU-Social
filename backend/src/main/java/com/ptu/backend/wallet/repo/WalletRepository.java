package com.ptu.backend.wallet.repo;

import java.util.Optional;
import java.util.UUID;

import com.ptu.backend.wallet.domain.Wallet;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

	Optional<Wallet> findByUserId(UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select wallet from Wallet wallet where wallet.userId = :userId")
	Optional<Wallet> findByUserIdForUpdate(@Param("userId") UUID userId);

	/**
	 * Creates a wallet for the user unless it already exists: avoids a race between
	 * concurrent credits when the wallet is created lazily.
	 */
	@Modifying
	@Query(value = "insert into wallets (id, user_id, currency_balance, created_at, updated_at) "
			+ "values (gen_random_uuid(), :userId, 0, now(), now()) on conflict (user_id) do nothing",
			nativeQuery = true)
	void insertIfAbsent(@Param("userId") UUID userId);
}
