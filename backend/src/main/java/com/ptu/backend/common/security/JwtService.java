package com.ptu.backend.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.ptu.backend.config.AppProperties;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final Duration ttl;

	public JwtService(AppProperties properties) {
		byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < 32) {
			throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
		}
		this.key = Keys.hmacShaKeyFor(secret);
		this.ttl = properties.jwt().ttl();
	}

	public String generate(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(ttl)))
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	public UserPrincipal parse(String token) {
		Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
		return new UserPrincipal(
				UUID.fromString(claims.getSubject()),
				claims.get("email", String.class),
				Role.valueOf(claims.get("role", String.class)));
	}

	public long ttlSeconds() {
		return ttl.toSeconds();
	}
}
