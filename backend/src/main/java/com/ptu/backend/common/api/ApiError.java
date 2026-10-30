package com.ptu.backend.common.api;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

public record ApiError(Instant timestamp, int status, String error, String message, String path) {

	public static ApiError of(int status, String error, String message, HttpServletRequest request) {
		return new ApiError(Instant.now(), status, error, message, request == null ? null : request.getRequestURI());
	}
}
