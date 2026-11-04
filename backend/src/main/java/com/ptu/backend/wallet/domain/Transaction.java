package com.ptu.backend.wallet.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "wallet_id", nullable = false, updatable = false)
	private UUID walletId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 32)
	private String type;

	@Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
	private String idempotencyKey;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public Transaction(UUID walletId, BigDecimal amount, String type, String idempotencyKey) {
		this.walletId = walletId;
		this.amount = amount;
		this.type = type;
		this.idempotencyKey = idempotencyKey;
	}
}
