package com.ptu.backend.common.api;

import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ptu.backend.common.exception.ApiException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
		HttpStatus status = ex.getStatus();
		return ResponseEntity.status(status)
				.body(ApiError.of(status.value(), status.getReasonPhrase(), ex.getMessage(), request));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.badRequest()
				.body(ApiError.of(400, "Bad Request", message.isBlank() ? "Validation failed" : message, request));
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, IllegalArgumentException.class })
	public ResponseEntity<ApiError> handleBadArguments(Exception ex, HttpServletRequest request) {
		return ResponseEntity.badRequest().body(ApiError.of(400, "Bad Request", ex.getMessage(), request));
	}

	@ExceptionHandler(org.springframework.data.redis.RedisConnectionFailureException.class)
	public ResponseEntity<ApiError> handleRedisUnavailable(Exception ex, HttpServletRequest request) {
		log.error("Redis is unavailable", ex);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiError.of(503, "Service Unavailable", "Сервис временно недоступен, попробуйте позже", request));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of(409, "Conflict", "Запись уже существует или нарушает ограничения", request));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of(500, "Internal Server Error", "Внутренняя ошибка сервера", request));
	}
}
