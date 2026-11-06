package com.ptu.backend.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		String baseUrl,
		String mailFrom,
		Cors cors,
		Registration registration,
		Invite invite,
		Verification verification,
		Jwt jwt,
		AdminBootstrap adminBootstrap,
		Seed seed,
		RateLimit rateLimit,
		Reviews reviews) {

	public record Cors(List<String> allowedOrigins) {
	}

	public record Registration(Duration ttl) {
	}

	public record Invite(Duration ttl) {
	}

	public record Verification(
			Duration codeTtl,
			int maxAttempts,
			int maxSends,
			Duration sendWindow,
			Duration resendInterval) {
	}

	public record Jwt(String secret, Duration ttl) {
	}

	public record AdminBootstrap(String email, String password, String fullName) {
	}

	public record Seed(String officeEmail, String officePassword, String adminEmail, String adminPassword) {
	}

	public record RateLimit(boolean enabled, Limit login, Limit authenticated, Limit anonymous) {
	}

	public record Limit(int requests, Duration window) {
	}

	public record Reviews(BigDecimal rewardAmount, int monthlyLimit) {
	}
}
