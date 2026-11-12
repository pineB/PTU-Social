package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class RateLimitIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StringRedisTemplate redis;

	@BeforeEach
	void resetRateLimits() {
		Set<String> keys = redis.keys("rate_limit:*");
		if (keys != null && !keys.isEmpty()) {
			redis.delete(keys);
		}
	}

	@Test
	void sixthLoginAttemptReturns429WithApiErrorAndRetryAfter() throws Exception {
		String wrongLogin = json(Map.of("email", "nobody-" + uniqueEmail("login"), "password", "wrong12345"));

		for (int attempt = 1; attempt <= 5; attempt++) {
			mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(wrongLogin))
					.andExpect(status().isUnauthorized())
					.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
					.andExpect(jsonPath("$.status").value(401))
					.andExpect(jsonPath("$.error").value("Unauthorized"))
					.andExpect(jsonPath("$.path").value("/api/auth/login"));
		}

		MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(wrongLogin))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.error").value("Too Many Requests"))
				.andExpect(jsonPath("$.message").value("Слишком много запросов, попробуйте позже"))
				.andExpect(jsonPath("$.path").value("/api/auth/login"))
				.andReturn();

		long retryAfter = Long.parseLong(result.getResponse().getHeader(HttpHeaders.RETRY_AFTER));
		assertThat(retryAfter).isBetween(1L, 60L);
	}

	@Test
	void unauthenticatedRequestReturnsApiError() throws Exception {
		mockMvc.perform(get("/api/academic/registration-requests"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Требуется аутентификация"))
				.andExpect(jsonPath("$.path").value("/api/academic/registration-requests"))
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void forbiddenRequestReturnsApiError() throws Exception {
		User student = createUser(uniqueEmail("student"), "student12345", Role.STUDENT);

		mockMvc.perform(post("/api/academic/teacher-invites")
						.header(HttpHeaders.AUTHORIZATION, bearer(student))
						.contentType(APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.error").value("Forbidden"))
				.andExpect(jsonPath("$.message").value("Недостаточно прав"))
				.andExpect(jsonPath("$.path").value("/api/academic/teacher-invites"))
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void authenticatedUserWriteLimitIsEnforcedAfter120Requests() throws Exception {
		User student = createUser(uniqueEmail("student"), "student12345", Role.STUDENT);
		String authorization = bearer(student);
		awaitFreshWindow();

		for (int request = 1; request <= 120; request++) {
			mockMvc.perform(post("/api/academic/teacher-invites")
							.header(HttpHeaders.AUTHORIZATION, authorization)
							.contentType(APPLICATION_JSON)
							.content("{}"))
					.andExpect(status().isForbidden());
		}

		mockMvc.perform(post("/api/academic/teacher-invites")
						.header(HttpHeaders.AUTHORIZATION, authorization)
						.contentType(APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.error").value("Too Many Requests"))
				.andExpect(jsonPath("$.path").value("/api/academic/teacher-invites"));
	}

	@Test
	void authenticatedUserReadsUseSeparateHigherBudget() throws Exception {
		User office = createUser(uniqueEmail("office"), "office12345", Role.ACADEMIC_OFFICE);
		String authorization = bearer(office);
		awaitFreshWindow();

		// 130 reads exceed the write limit (120) but stay within the read budget (600).
		for (int request = 1; request <= 130; request++) {
			mockMvc.perform(get("/api/academic/registration-requests").header(HttpHeaders.AUTHORIZATION, authorization))
					.andExpect(status().isOk());
		}

		// Reads must not consume the write budget: the request reaches the controller and fails
		// validation (400) instead of being rejected by the limiter (429).
		mockMvc.perform(post("/api/academic/teacher-invites")
						.header(HttpHeaders.AUTHORIZATION, authorization)
						.contentType(APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	/**
	 * Fixed window is one minute long, so make sure the whole burst fits into a single window.
	 */
	private static void awaitFreshWindow() throws InterruptedException {
		long millisLeftInWindow = 60_000 - (System.currentTimeMillis() % 60_000);
		if (millisLeftInWindow < 20_000) {
			Thread.sleep(millisLeftInWindow + 100);
		}
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
