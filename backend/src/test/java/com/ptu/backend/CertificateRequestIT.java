package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class CertificateRequestIT extends AbstractIntegrationTest {

	private static final String PASSWORD = "Password123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private User student;
	private User otherStudent;
	private User office;

	@BeforeEach
	void setUp() {
		student = createUser(uniqueEmail("student"), PASSWORD, Role.STUDENT);
		otherStudent = createUser(uniqueEmail("student"), PASSWORD, Role.STUDENT);
		office = createUser(uniqueEmail("office"), PASSWORD, Role.ACADEMIC_OFFICE);
	}

	@Test
	void studentCreatesAndSeesOwnRequest() throws Exception {
		UUID id = createRequest(student, "EDUCATION");

		mockMvc.perform(get("/api/certificates/my").header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(id.toString()))
				.andExpect(jsonPath("$[0].studentId").value(student.getId().toString()))
				.andExpect(jsonPath("$[0].studentName").value(student.getFullName()))
				.andExpect(jsonPath("$[0].type").value("EDUCATION"))
				.andExpect(jsonPath("$[0].status").value("PENDING"))
				.andExpect(jsonPath("$[0].rejectionReason").doesNotExist())
				.andExpect(jsonPath("$[0].createdAt").exists());
	}

	@Test
	void studentSeesOnlyOwnRequests() throws Exception {
		UUID mine = createRequest(student, "INCOME");
		createRequest(otherStudent, "MILITARY");

		mockMvc.perform(get("/api/certificates/my").header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(mine.toString()));
	}

	@Test
	void academicOfficeApprovesRequest() throws Exception {
		UUID id = createRequest(student, "EDUCATION");

		mockMvc.perform(patch("/api/certificates/{id}/status", id)
						.header(HttpHeaders.AUTHORIZATION, bearer(office))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"APPROVED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.rejectionReason").doesNotExist());

		mockMvc.perform(get("/api/certificates/my").header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("APPROVED"));
	}

	@Test
	void academicOfficeRejectsWithReason() throws Exception {
		UUID id = createRequest(student, "INCOME");

		mockMvc.perform(patch("/api/certificates/{id}/status", id)
						.header(HttpHeaders.AUTHORIZATION, bearer(office))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"REJECTED\",\"rejectionReason\":\"Не оплачена сессия\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.rejectionReason").value("Не оплачена сессия"));
	}

	@Test
	void rejectWithoutReasonReturns400() throws Exception {
		UUID id = createRequest(student, "MILITARY");

		mockMvc.perform(patch("/api/certificates/{id}/status", id)
						.header(HttpHeaders.AUTHORIZATION, bearer(office))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"REJECTED\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Причина отклонения обязательна"));

		mockMvc.perform(patch("/api/certificates/{id}/status", id)
						.header(HttpHeaders.AUTHORIZATION, bearer(office))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"REJECTED\",\"rejectionReason\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Причина отклонения обязательна"));
	}

	@Test
	void studentCannotChangeStatus() throws Exception {
		UUID id = createRequest(student, "EDUCATION");

		mockMvc.perform(patch("/api/certificates/{id}/status", id)
						.header(HttpHeaders.AUTHORIZATION, bearer(student))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"APPROVED\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.error").value("Forbidden"))
				.andExpect(jsonPath("$.message").value("Недостаточно прав"));
	}

	@Test
	void studentCannotListAllRequests() throws Exception {
		mockMvc.perform(get("/api/certificates").header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void officeListsRequestsWithStatusFilter() throws Exception {
		UUID pending = createRequest(student, "EDUCATION");
		createRequest(otherStudent, "INCOME");
		UUID approved = createRequest(student, "MILITARY");

		mockMvc.perform(patch("/api/certificates/{id}/status", approved)
						.header(HttpHeaders.AUTHORIZATION, bearer(office))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"APPROVED\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/certificates").header(HttpHeaders.AUTHORIZATION, bearer(office)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(3));

		mockMvc.perform(get("/api/certificates").param("status", "PENDING")
						.header(HttpHeaders.AUTHORIZATION, bearer(office)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].id").value(pending.toString()));
	}

	@Test
	void processedRequestCannotBeChangedAgain() throws Exception {
		UUID id = createRequest(student, "EDUCATION");

		mockMvc.perform(patch("/api/certificates/{id}/status", id)
						.header(HttpHeaders.AUTHORIZATION, bearer(office))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"APPROVED\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/certificates/{id}/status", id)
						.header(HttpHeaders.AUTHORIZATION, bearer(office))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"REJECTED\",\"rejectionReason\":\"поздно\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Заявка уже обработана"));
	}

	private UUID createRequest(User owner, String type) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/certificates")
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\":\"" + type + "\",\"deliveryMethod\":\"PICKUP\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.type").value(type))
				.andExpect(jsonPath("$.studentId").value(owner.getId().toString()))
				.andReturn();
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		UUID id = UUID.fromString(body.get("id").asText());
		assertThat(id).isNotNull();
		return id;
	}
}
