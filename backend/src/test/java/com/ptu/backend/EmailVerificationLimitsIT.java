package com.ptu.backend;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.academic.domain.Faculty;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.mail.MailService;
import com.ptu.backend.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class EmailVerificationLimitsIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StringRedisTemplate redis;

	@MockitoBean
	private MailService mailService;

	@Test
	void resendIntervalAndSendLimitAreEnforced() throws Exception {
		UUID requestId = createRequest();

		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-code", requestId))
				.andExpect(status().isTooManyRequests());

		redis.delete("verification:registration:resend:" + requestId);
		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-code", requestId))
				.andExpect(status().isAccepted());

		redis.delete("verification:registration:resend:" + requestId);
		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-code", requestId))
				.andExpect(status().isAccepted());

		redis.delete("verification:registration:resend:" + requestId);
		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-code", requestId))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void wrongCodeAttemptsAreLimited() throws Exception {
		UUID requestId = createRequest();
		String wrongCode = "000000";

		for (int attempt = 1; attempt <= 5; attempt++) {
			mockMvc.perform(post("/api/auth/registration-requests/{id}/email-verify", requestId)
					.contentType(APPLICATION_JSON)
					.content(json(Map.of("code", wrongCode))))
					.andExpect(status().isBadRequest());
		}
		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-verify", requestId)
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("code", wrongCode))))
				.andExpect(status().isTooManyRequests());

		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-verify", requestId)
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("code", wrongCode))))
				.andExpect(status().isBadRequest());
	}

	private UUID createRequest() throws Exception {
		Faculty faculty = createFaculty("ФИТ " + UUID.randomUUID());
		StudyGroup group = createGroup("ИС-" + UUID.randomUUID(), faculty);
		MvcResult result = mockMvc.perform(post("/api/auth/registration-requests")
				.contentType(APPLICATION_JSON)
				.content(json(Map.of(
						"fullName", "Пётр Петров",
						"email", uniqueEmail("limits"),
						"password", "passw0rd1",
						"facultyId", faculty.getId(),
						"groupId", group.getId()))))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
