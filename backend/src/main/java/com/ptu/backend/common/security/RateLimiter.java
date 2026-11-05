package com.ptu.backend.common.security;

import java.time.Duration;

import com.ptu.backend.config.AppProperties;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Fixed-window rate limiter backed by Redis counters.
 * The window index is part of the key, so counters never need to be reset manually.
 */
@Slf4j
@Component
public class RateLimiter {

	private static final String KEY_PREFIX = "rate_limit:";

	private final StringRedisTemplate redis;

	public RateLimiter(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public RateLimitDecision acquire(String scope, String identity, AppProperties.Limit limit) {
		long windowSeconds = Math.max(limit.window().toSeconds(), 1);
		long now = System.currentTimeMillis() / 1000;
		long windowIndex = now / windowSeconds;
		long secondsLeft = windowSeconds - (now % windowSeconds);
		String key = KEY_PREFIX + scope + ":" + identity + ":" + windowIndex;

		try {
			Long count = redis.opsForValue().increment(key);
			if (count != null && count == 1L) {
				redis.expire(key, Duration.ofSeconds(windowSeconds));
			}
			if (count == null) {
				return RateLimitDecision.permit();
			}
			return count <= limit.requests()
					? RateLimitDecision.permit()
					: RateLimitDecision.deny(secondsLeft);
		}
		catch (DataAccessException ex) {
			log.warn("Rate limiter is unavailable, allowing request: {}", ex.getMessage());
			return RateLimitDecision.permit();
		}
	}

	public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

		static RateLimitDecision permit() {
			return new RateLimitDecision(true, 0);
		}

		static RateLimitDecision deny(long retryAfterSeconds) {
			return new RateLimitDecision(false, retryAfterSeconds);
		}
	}
}
