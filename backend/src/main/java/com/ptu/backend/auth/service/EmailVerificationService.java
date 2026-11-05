package com.ptu.backend.auth.service;

import java.security.SecureRandom;
import java.util.UUID;

import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.util.Hashing;
import com.ptu.backend.config.AppProperties;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Stores hashed email verification codes in Redis together with send/attempt counters.
 * All operations are fail-closed: if Redis is unavailable verification is not performed.
 */
@Slf4j
@Service
public class EmailVerificationService {

	private static final String CODE_KEY = "email_verification:";
	private static final String SENDS_KEY = "email_verification:sends:";
	private static final String ATTEMPTS_KEY = "email_verification:attempts:";
	private static final String RESEND_KEY = "email_verification:resend:";

	private final StringRedisTemplate redis;
	private final AppProperties properties;
	private final SecureRandom random = new SecureRandom();

	public EmailVerificationService(StringRedisTemplate redis, AppProperties properties) {
		this.redis = redis;
		this.properties = properties;
	}

	/**
	 * Issues a new code, invalidating the previous one. Returns the plain code to be sent by email.
	 */
	public String issueCode(UUID requestId) {
		AppProperties.Verification limits = properties.verification();
		String sendsKey = SENDS_KEY + requestId;

		int sends = parseInt(redis.opsForValue().get(sendsKey));
		if (sends >= limits.maxSends()) {
			throw ApiException.tooManyRequests("Слишком много отправок кода, попробуйте позже");
		}

		Boolean acquired = redis.opsForValue().setIfAbsent(RESEND_KEY + requestId, "1", limits.resendInterval());
		if (!Boolean.TRUE.equals(acquired)) {
			throw ApiException.tooManyRequests(
					"Повторная отправка кода возможна не раньше чем через %d секунд"
							.formatted(limits.resendInterval().toSeconds()));
		}

		Long updated = redis.opsForValue().increment(sendsKey);
		if (updated != null && updated == 1L) {
			redis.expire(sendsKey, limits.sendWindow());
		}

		String code = String.format("%06d", random.nextInt(1_000_000));
		String salt = Hashing.randomSalt();
		redis.opsForValue().set(CODE_KEY + requestId, salt + ":" + Hashing.sha256Hex(salt + code),
				limits.codeTtl());
		redis.delete(ATTEMPTS_KEY + requestId);
		return code;
	}

	public void verifyCode(UUID requestId, String code) {
		AppProperties.Verification limits = properties.verification();
		String codeKey = CODE_KEY + requestId;
		String stored = redis.opsForValue().get(codeKey);
		if (stored == null) {
			throw ApiException.badRequest("Код истёк или не был отправлен");
		}

		String attemptsKey = ATTEMPTS_KEY + requestId;
		Long attempts = redis.opsForValue().increment(attemptsKey);
		if (attempts != null && attempts == 1L) {
			redis.expire(attemptsKey, limits.codeTtl());
		}
		if (attempts != null && attempts > limits.maxAttempts()) {
			invalidate(requestId);
			throw ApiException.tooManyRequests("Превышено число попыток проверки кода, запросите новый код");
		}

		int separator = stored.indexOf(':');
		String salt = stored.substring(0, separator);
		String expectedHash = stored.substring(separator + 1);
		if (!Hashing.constantTimeEquals(expectedHash, Hashing.sha256Hex(salt + code))) {
			throw ApiException.badRequest("Неверный код подтверждения");
		}

		invalidate(requestId);
	}

	public void invalidate(UUID requestId) {
		redis.delete(CODE_KEY + requestId);
		redis.delete(ATTEMPTS_KEY + requestId);
	}

	private static int parseInt(String value) {
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException ex) {
			return 0;
		}
	}
}
