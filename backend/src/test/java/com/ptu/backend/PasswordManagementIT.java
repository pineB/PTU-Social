package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.mail.MailService;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.rate-limit.enabled=false")
class PasswordManagementIT extends AbstractIntegrationTest {

	private static final String OLD_PASSWORD = "oldpass1";
	private static final String NEW_PASSWORD = "newpass2";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StringRedisTemplate redis;

	@MockitoBean
	private MailService mailService;

	@Test
	void changePasswordUpdatesCredentials() throws Exception {
		User user = createUser(uniqueEmail("change"), OLD_PASSWORD, Role.STUDENT);

		changePassword(user, OLD_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

		User updated = userRepository.findById(user.getId()).orElseThrow();
		assertThat(passwordEncoder.matches(NEW_PASSWORD, updated.getPasswordHash())).isTrue();
		login(user.getEmail(), NEW_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void changePasswordRejectsWrongCurrentPassword() throws Exception {
		User user = createUser(uniqueEmail("wrong-current"), OLD_PASSWORD, Role.STUDENT);

		changePassword(user, "another1", NEW_PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void changePasswordRejectsSamePassword() throws Exception {
		User user = createUser(uniqueEmail("same"), OLD_PASSWORD, Role.STUDENT);

		changePassword(user, OLD_PASSWORD, OLD_PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void changePasswordRejectsWeakPassword() throws Exception {
		User user = createUser(uniqueEmail("weak"), OLD_PASSWORD, Role.STUDENT);

		changePassword(user, OLD_PASSWORD, "onlyletters").andExpect(status().isBadRequest());
	}

	@Test
	void changePasswordRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/auth/password/change")
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("currentPassword", OLD_PASSWORD, "newPassword", NEW_PASSWORD))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void forgotPasswordHidesUnknownEmail() throws Exception {
		mockMvc.perform(post("/api/auth/password/forgot")
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("email", uniqueEmail("unknown")))))
				.andExpect(status().isAccepted());

		verify(mailService, never()).sendPasswordResetCode(any(), any(), any(), any(Duration.class));
	}

	@Test
	void resetChangesPasswordAndConsumesCode() throws Exception {
		User user = createUser(uniqueEmail("reset"), OLD_PASSWORD, Role.STUDENT);
		String code = requestResetCode(user);

		reset(user.getEmail(), code, NEW_PASSWORD).andExpect(status().isNoContent());
		login(user.getEmail(), NEW_PASSWORD).andExpect(status().isOk());

		reset(user.getEmail(), code, "another3").andExpect(status().isBadRequest());
	}

	@Test
	void resetRejectsWrongCode() throws Exception {
		User user = createUser(uniqueEmail("reset-wrong"), OLD_PASSWORD, Role.STUDENT);
		String code = requestResetCode(user);
		String wrongCode = code.equals("000000") ? "000001" : "000000";

		reset(user.getEmail(), wrongCode, NEW_PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void resetLimitsWrongCodeAttempts() throws Exception {
		User user = createUser(uniqueEmail("reset-attempts"), OLD_PASSWORD, Role.STUDENT);
		String code = requestResetCode(user);
		String wrongCode = code.equals("000000") ? "000001" : "000000";

		for (int attempt = 1; attempt <= 5; attempt++) {
			reset(user.getEmail(), wrongCode, NEW_PASSWORD).andExpect(status().isBadRequest());
		}
		reset(user.getEmail(), wrongCode, NEW_PASSWORD).andExpect(status().isTooManyRequests());
	}

	@Test
	void forgotPasswordLimitsSends() throws Exception {
		User user = createUser(uniqueEmail("reset-sends"), OLD_PASSWORD, Role.STUDENT);

		for (int send = 1; send <= 4; send++) {
			redis.delete("verification:password-reset:resend:" + user.getId());
			mockMvc.perform(post("/api/auth/password/forgot")
					.contentType(APPLICATION_JSON)
					.content(json(Map.of("email", user.getEmail()))))
					.andExpect(status().isAccepted());
		}

		verify(mailService, times(3)).sendPasswordResetCode(eq(user.getEmail()), any(), any(), any(Duration.class));
	}

	private org.springframework.test.web.servlet.ResultActions changePassword(User user, String currentPassword,
			String newPassword) throws Exception {
		return mockMvc.perform(post("/api/auth/password/change")
				.header("Authorization", bearer(user))
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("currentPassword", currentPassword, "newPassword", newPassword))));
	}

	private org.springframework.test.web.servlet.ResultActions reset(String email, String code, String newPassword)
			throws Exception {
		return mockMvc.perform(post("/api/auth/password/reset")
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("email", email, "code", code, "newPassword", newPassword))));
	}

	private org.springframework.test.web.servlet.ResultActions login(String email, String password)
			throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("email", email, "password", password))));
	}

	private String requestResetCode(User user) throws Exception {
		mockMvc.perform(post("/api/auth/password/forgot")
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("email", user.getEmail()))))
				.andExpect(status().isAccepted());
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendPasswordResetCode(eq(user.getEmail()), any(), captor.capture(), any(Duration.class));
		return captor.getValue();
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
