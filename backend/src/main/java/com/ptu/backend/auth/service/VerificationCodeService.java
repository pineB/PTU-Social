package com.ptu.backend.auth.service;

import java.security.SecureRandom;

import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.util.Hashing;
import com.ptu.backend.config.AppProperties;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Stores hashed one-time codes in Redis together with send/attempt counters.
 * Codes are namespaced by scope (registration, password-reset) and subject id.
 * All operations are fail-closed: if Redis is unavailable verification is not performed.
 */
@Slf4j
@Service
public class VerificationCodeService {

	public static final String SCOPE_REGISTRATION = "registration";
	public static final String SCOPE_PASSWORD_RESET = "password-reset";

	private static final String KEY_PREFIX = "verification:";

	private final StringRedisTemplate redis;
	private final AppProperties properties;
	private final SecureRandom random = new SecureRandom();

	public VerificationCodeService(StringRedisTemplate redis, AppProperties properties) {
		this.redis = redis;
		this.properties = properties;
	}

	/**
	 * Issues a new code, invalidating the previous one. Returns the plain code to be sent by email.
	 */
	public String issueCode(String scope, String subject) {
		AppProperties.Verification limits = properties.verification();
		String sendsKey = key(scope, "sends", subject);

		int sends = parseInt(redis.opsForValue().get(sendsKey));
		if (sends >= limits.maxSends()) {
			throw ApiException.tooManyRequests("Слишком много отправок кода, попробуйте позже");
		}

		Boolean acquired = redis.opsForValue().setIfAbsent(key(scope, "resend", subject), "1",
				limits.resendInterval());
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
		redis.opsForValue().set(key(scope, "code", subject),
				salt + ":" + Hashing.sha256Hex(salt + code),
				limits.codeTtl());
		redis.delete(key(scope, "attempts", subject));
		return code;
	}

	public void verifyCode(String scope, String subject, String code) {
		AppProperties.Verification limits = properties.verification();
		String codeKey = key(scope, "code", subject);
		String stored = redis.opsForValue().get(codeKey);
		if (stored == null) {
			throw ApiException.badRequest("Код истёк или не был отправлен");
		}

		String attemptsKey = key(scope, "attempts", subject);
		Long attempts = redis.opsForValue().increment(attemptsKey);
		if (attempts != null && attempts == 1L) {
			redis.expire(attemptsKey, limits.codeTtl());
		}
		if (attempts != null && attempts > limits.maxAttempts()) {
			invalidate(scope, subject);
			throw ApiException.tooManyRequests("Превышено число попыток проверки кода, запросите новый код");
		}

		int separator = stored.indexOf(':');
		String salt = stored.substring(0, separator);
		String expectedHash = stored.substring(separator + 1);
		if (!Hashing.constantTimeEquals(expectedHash, Hashing.sha256Hex(salt + code))) {
			throw ApiException.badRequest("Неверный код подтверждения");
		}

		invalidate(scope, subject);
	}

	public void invalidate(String scope, String subject) {
		redis.delete(key(scope, "code", subject));
		redis.delete(key(scope, "attempts", subject));
	}

	private static String key(String scope, String kind, String subject) {
		return KEY_PREFIX + scope + ":" + kind + ":" + subject;
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
