package com.ptu.backend.common.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.common.api.ApiErrorWriter;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Returns the unified {@link com.ptu.backend.common.api.ApiError} JSON for unauthenticated requests.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		ApiErrorWriter.write(objectMapper, request, response, HttpStatus.UNAUTHORIZED, "Требуется аутентификация");
	}
}
