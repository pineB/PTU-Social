package com.ptu.backend.common.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.common.api.ApiErrorWriter;
import com.ptu.backend.config.AppProperties;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies Redis-backed rate limits to the public API: login attempts per IP, requests of
 * authenticated users per user id, requests of anonymous callers per IP.
 * Registered inside the Spring Security chain (right after the JWT authentication filter).
 */
public class RateLimitFilter extends OncePerRequestFilter {

	static final String LOGIN_PATH = "/api/auth/login";
	static final String API_PREFIX = "/api/";

	private final RateLimiter rateLimiter;
	private final AppProperties properties;
	private final ObjectMapper objectMapper;

	public RateLimitFilter(RateLimiter rateLimiter, AppProperties properties, ObjectMapper objectMapper) {
		this.rateLimiter = rateLimiter;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		AppProperties.RateLimit limits = properties.rateLimit();
		String path = request.getRequestURI();
		if (limits == null || !limits.enabled() || !path.startsWith(API_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		boolean readOnly = isReadOnly(request);
		String scope;
		String identity;
		AppProperties.Limit limit;
		if (HttpMethod.POST.matches(request.getMethod()) && LOGIN_PATH.equals(path)) {
			scope = "login";
			identity = clientIp(request);
			limit = limits.login();
		}
		else if (currentPrincipal() instanceof UserPrincipal principal) {
			// Reads (GET/HEAD) are limited by a separate, much higher budget: one page may load
			// dozens of images, while state-changing requests stay at the spec limit.
			scope = readOnly ? "user-read" : "user";
			identity = principal.id().toString();
			limit = readOnly ? limits.authenticatedReadOrFallback() : limits.authenticated();
		}
		else {
			scope = readOnly ? "ip-read" : "ip";
			identity = clientIp(request);
			limit = readOnly ? limits.anonymousReadOrFallback() : limits.anonymous();
		}

		RateLimiter.RateLimitDecision decision = rateLimiter.acquire(scope, identity, limit);
		if (!decision.allowed()) {
			response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
			ApiErrorWriter.write(objectMapper, request, response, HttpStatus.TOO_MANY_REQUESTS,
					"Слишком много запросов, попробуйте позже");
			return;
		}
		filterChain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/actuator/") || !path.startsWith(API_PREFIX);
	}

	private static boolean isReadOnly(HttpServletRequest request) {
		return HttpMethod.GET.matches(request.getMethod()) || HttpMethod.HEAD.matches(request.getMethod());
	}

	private static Object currentPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null ? null : authentication.getPrincipal();
	}

	private static String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
