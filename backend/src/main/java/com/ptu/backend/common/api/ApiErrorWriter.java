package com.ptu.backend.common.api;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Writes the unified {@link ApiError} payload for errors produced outside of the MVC layer
 * (Spring Security entry points, servlet filters).
 */
public final class ApiErrorWriter {

	private ApiErrorWriter() {
	}

	public static void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
			HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(),
				ApiError.of(status.value(), status.getReasonPhrase(), message, request));
	}
}
