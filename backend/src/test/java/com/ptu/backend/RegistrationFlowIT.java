package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.academic.domain.Faculty;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.auth.domain.RegistrationRequest;
import com.ptu.backend.auth.repo.RegistrationRequestRepository;
import com.ptu.backend.mail.MailService;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.StudentProfileRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class RegistrationFlowIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RegistrationRequestRepository registrationRequests;

	@Autowired
	private StudentProfileRepository studentProfiles;

	@MockitoBean
	private MailService mailService;

	@Test
	void studentRegistrationFullCycle() throws Exception {
		Faculty faculty = createFaculty("ФИТ " + UUID.randomUUID());
		StudyGroup group = createGroup("ИС-" + UUID.randomUUID(), faculty);
		String email = uniqueEmail("student");
		UUID requestId = createRequest(email, faculty, group);

		String code = lastVerificationCode(email);
		String wrongCode = code.equals("000000") ? "000001" : "000000";

		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-verify", requestId)
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("code", wrongCode))))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-verify", requestId)
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("code", code))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EMAIL_VERIFIED"));

		User office = createUser(uniqueEmail("office"), "office12345", Role.ACADEMIC_OFFICE);

		mockMvc.perform(post("/api/academic/registration-requests/{id}/approve", requestId)
				.header("Authorization", bearer(office)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));

		User student = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		assertThat(student.getRole()).isEqualTo(Role.STUDENT);
		assertThat(studentProfiles.findByUserId(student.getId())).isPresent()
				.get()
				.satisfies(profile -> assertThat(profile.getGroupId()).isEqualTo(group.getId()));
	}

	@Test
	void rejectedRequestClearsPasswordAndFreesEmail() throws Exception {
		Faculty faculty = createFaculty("ФЭ " + UUID.randomUUID());
		StudyGroup group = createGroup("ЭК-" + UUID.randomUUID(), faculty);
		String email = uniqueEmail("rejected");
		UUID requestId = createRequest(email, faculty, group);
		verifyEmail(requestId, lastVerificationCode(email));

		User office = createUser(uniqueEmail("office"), "office12345", Role.ACADEMIC_OFFICE);
		mockMvc.perform(post("/api/academic/registration-requests/{id}/reject", requestId)
				.header("Authorization", bearer(office)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"));

		RegistrationRequest request = registrationRequests.findById(requestId).orElseThrow();
		assertThat(request.getPasswordHash()).isNull();
		assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
	}

	@Test
	void duplicateActiveRegistrationIsRejected() throws Exception {
		Faculty faculty = createFaculty("ФЭ " + UUID.randomUUID());
		StudyGroup group = createGroup("ЭК-" + UUID.randomUUID(), faculty);
		String email = uniqueEmail("duplicate");
		createRequest(email, faculty, group);

		mockMvc.perform(post("/api/auth/registration-requests")
				.contentType(APPLICATION_JSON)
				.content(json(registrationPayload(email, faculty, group))))
				.andExpect(status().isConflict());
	}

	@Test
	void approveBeforeEmailVerificationIsRejected() throws Exception {
		Faculty faculty = createFaculty("ФЭ " + UUID.randomUUID());
		StudyGroup group = createGroup("ЭК-" + UUID.randomUUID(), faculty);
		UUID requestId = createRequest(uniqueEmail("unverified"), faculty, group);

		User office = createUser(uniqueEmail("office"), "office12345", Role.ACADEMIC_OFFICE);
		mockMvc.perform(post("/api/academic/registration-requests/{id}/approve", requestId)
				.header("Authorization", bearer(office)))
				.andExpect(status().isConflict());
	}

	private UUID createRequest(String email, Faculty faculty, StudyGroup group) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/registration-requests")
				.contentType(APPLICATION_JSON)
				.content(json(registrationPayload(email, faculty, group))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING_EMAIL"))
				.andReturn();
		return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
	}

	private void verifyEmail(UUID requestId, String code) throws Exception {
		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-verify", requestId)
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("code", code))))
				.andExpect(status().isOk());
	}

	private Map<String, Object> registrationPayload(String email, Faculty faculty, StudyGroup group) {
		return Map.of(
				"fullName", "Иван Иванов",
				"email", email,
				"password", "passw0rd1",
				"facultyId", faculty.getId(),
				"groupId", group.getId());
	}

	private String lastVerificationCode(String email) {
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendVerificationCode(eq(email), any(), captor.capture(), any(Duration.class));
		return captor.getValue();
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
