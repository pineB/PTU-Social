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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
public class Wallet {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false, unique = true, updatable = false)
	private UUID userId;

	@Column(name = "currency_balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal currencyBalance = BigDecimal.ZERO;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Wallet(UUID userId) {
		this.userId = userId;
		this.currencyBalance = BigDecimal.ZERO;
	}
}
