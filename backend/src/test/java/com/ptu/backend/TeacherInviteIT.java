package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.academic.domain.AssignmentStatus;
import com.ptu.backend.academic.domain.Department;
import com.ptu.backend.academic.domain.Discipline;
import com.ptu.backend.academic.domain.Faculty;
import com.ptu.backend.academic.domain.Semester;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.repo.DepartmentRepository;
import com.ptu.backend.academic.repo.DisciplineRepository;
import com.ptu.backend.academic.repo.SemesterRepository;
import com.ptu.backend.academic.repo.TeacherAssignmentRepository;
import com.ptu.backend.auth.dto.AcceptTeacherInviteRequest;
import com.ptu.backend.auth.dto.CreateTeacherInviteRequest;
import com.ptu.backend.common.util.Hashing;
import com.ptu.backend.mail.MailService;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class TeacherInviteIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private DisciplineRepository disciplineRepository;

	@Autowired
	private SemesterRepository semesterRepository;

	@Autowired
	private TeacherAssignmentRepository teacherAssignmentRepository;

	@MockitoBean
	private MailService mailService;

	private Department department;
	private StudyGroup group;
	private Discipline discipline;
	private String officeBearer;
	private String semesterName;

	@BeforeEach
	void setUp() {
		Faculty faculty = createFaculty("Инженерный факультет " + UUID.randomUUID());
		group = createGroup("ИС-21-" + UUID.randomUUID(), faculty);
		department = departmentRepository.save(new Department("Кафедра ИТ " + UUID.randomUUID()));
		discipline = disciplineRepository.save(new Discipline("Базы данных " + UUID.randomUUID(), department.getId()));
		semesterName = "2026/2027 осенний " + UUID.randomUUID();
		semesterRepository.save(new Semester(semesterName, LocalDate.of(2026, 9, 1),
				LocalDate.of(2027, 1, 31), true));
		officeBearer = bearer(createUser(uniqueEmail("office"), "office12345", Role.ACADEMIC_OFFICE));
	}

	@Test
	void officeInvitesTeacherAndTeacherAccepts() throws Exception {
		String email = uniqueEmail("teacher");
		String payload = invitePayload(email);

		mockMvc.perform(post("/api/academic/teacher-invites")
						.header("Authorization", officeBearer)
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.expiresAt").isNotEmpty());

		String token = capturedInviteToken(email);

		mockMvc.perform(get("/api/auth/teacher-invites/{token}", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.fullName").value("Иванов Иван Петрович"));

		mockMvc.perform(post("/api/auth/teacher-invites/{token}/accept", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AcceptTeacherInviteRequest("teacher12345"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.role").value("TEACHER"))
				.andExpect(jsonPath("$.expiresInSeconds").value(86_400));

		User teacher = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		assertThat(teacher.getRole()).isEqualTo(Role.TEACHER);
		UUID semesterId = semesterRepository.findByName(semesterName).orElseThrow().getId();
		assertThat(teacherAssignmentRepository.findGroupIds(teacher.getId(), semesterId, AssignmentStatus.ACTIVE))
				.containsExactly(group.getId());

		mockMvc.perform(get("/api/auth/teacher-invites/{token}", token))
				.andExpect(status().isGone());
	}

	@Test
	void duplicatePendingInviteIsRejected() throws Exception {
		String email = uniqueEmail("teacher");

		mockMvc.perform(post("/api/academic/teacher-invites")
						.header("Authorization", officeBearer)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invitePayload(email)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/academic/teacher-invites")
						.header("Authorization", officeBearer)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invitePayload(email)))
				.andExpect(status().isConflict());
	}

	@Test
	void teacherCannotCreateInvites() throws Exception {
		String teacherBearer = bearer(createUser(uniqueEmail("teacher"), "teacher12345", Role.TEACHER));

		mockMvc.perform(post("/api/academic/teacher-invites")
						.header("Authorization", teacherBearer)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invitePayload(uniqueEmail("invited"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void unknownTokenReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/auth/teacher-invites/{token}", Hashing.randomToken()))
				.andExpect(status().isNotFound());
	}

	private String invitePayload(String email) throws Exception {
		return objectMapper.writeValueAsString(new CreateTeacherInviteRequest("Иванов Иван Петрович", email,
				department.getId(), Set.of(discipline.getId()), Set.of(group.getId())));
	}

	private String capturedInviteToken(String email) {
		ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendTeacherInvite(eq(email), any(), link.capture(), any(Duration.class));
		String query = URI.create(link.getValue()).getQuery();
		assertThat(query).startsWith("token=");
		return URLDecoder.decode(query.substring("token=".length()), StandardCharsets.UTF_8);
	}
}
