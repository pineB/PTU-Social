package com.ptu.backend.common.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.common.api.ApiErrorWriter;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Returns the unified {@link com.ptu.backend.common.api.ApiError} JSON for forbidden requests.
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public RestAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		ApiErrorWriter.write(objectMapper, request, response, HttpStatus.FORBIDDEN, "Недостаточно прав");
	}
}
