package com.ptu.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.academic.domain.AssignmentStatus;
import com.ptu.backend.academic.domain.Department;
import com.ptu.backend.academic.domain.Discipline;
import com.ptu.backend.academic.domain.Faculty;
import com.ptu.backend.academic.domain.Semester;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.domain.TeacherAssignment;
import com.ptu.backend.academic.repo.DepartmentRepository;
import com.ptu.backend.academic.repo.DisciplineRepository;
import com.ptu.backend.academic.repo.SemesterRepository;
import com.ptu.backend.academic.repo.TeacherAssignmentRepository;
import com.ptu.backend.mail.MailService;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.StudentProfile;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.StudentProfileRepository;

import org.junit.jupiter.api.BeforeEach;
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
class RegistrationNotificationIT extends AbstractIntegrationTest {

	private static final String APPLICANT_NAME = "Иван Иванов";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StudentProfileRepository studentProfiles;

	@Autowired
	private TeacherAssignmentRepository teacherAssignments;

	@Autowired
	private DisciplineRepository disciplines;

	@Autowired
	private DepartmentRepository departments;

	@Autowired
	private SemesterRepository semesters;

	@MockitoBean
	private MailService mailService;

	private Faculty faculty;
	private StudyGroup group;
	private Semester semester;
	private Discipline discipline;

	@BeforeEach
	void setUp() {
		faculty = createFaculty("ФИТ " + UUID.randomUUID());
		group = createGroup("ИС-" + UUID.randomUUID(), faculty);
		semester = semesters.save(new Semester("2026/2027 " + UUID.randomUUID(), LocalDate.of(2026, 9, 1),
				LocalDate.of(2027, 1, 31), true));
		Department department = departments.save(new Department("Кафедра ИС " + UUID.randomUUID()));
		discipline = disciplines.save(new Discipline("Сети " + UUID.randomUUID(), department.getId()));
	}

	@Test
	void notifiesGroupTeacherAndLeaderOnly() throws Exception {
		User teacher = createUser(uniqueEmail("teacher"), "teacher12345", Role.TEACHER);
		assignTeacher(teacher, group, AssignmentStatus.ACTIVE);

		StudyGroup foreignGroup = createGroup("ЭК-" + UUID.randomUUID(), faculty);
		User foreignTeacher = createUser(uniqueEmail("foreign"), "teacher12345", Role.TEACHER);
		assignTeacher(foreignTeacher, foreignGroup, AssignmentStatus.ACTIVE);

		User leader = createUser(uniqueEmail("leader"), "leader12345", Role.STUDENT);
		StudentProfile profile = new StudentProfile();
		profile.setUserId(leader.getId());
		profile.setGroupId(group.getId());
		profile.setGroupLeader(true);
		studentProfiles.saveAndFlush(profile);

		String applicantEmail = uniqueEmail("applicant");
		verifyRequest(applicantEmail);

		verify(mailService).sendRegistrationPendingNotice(eq(teacher.getEmail()), any(), eq(APPLICANT_NAME),
				eq(group.getName()), eq("http://localhost/teacher/students"));
		verify(mailService).sendRegistrationPendingNotice(eq(leader.getEmail()), any(), eq(APPLICANT_NAME),
				eq(group.getName()), eq("http://localhost/student/group-confirmation"));
		verify(mailService, never()).sendRegistrationPendingNotice(eq(foreignTeacher.getEmail()), anyString(),
				anyString(), anyString(), anyString());
		verify(mailService, never()).sendRegistrationPendingNotice(eq(applicantEmail), anyString(), anyString(),
				anyString(), anyString());
	}

	@Test
	void archivedAssignmentIsNotNotified() throws Exception {
		User teacher = createUser(uniqueEmail("archived"), "teacher12345", Role.TEACHER);
		assignTeacher(teacher, group, AssignmentStatus.ARCHIVED);

		verifyRequest(uniqueEmail("applicant"));

		verify(mailService, never()).sendRegistrationPendingNotice(eq(teacher.getEmail()), anyString(), anyString(),
				anyString(), anyString());
	}

	private void assignTeacher(User teacher, StudyGroup targetGroup, AssignmentStatus status) {
		TeacherAssignment assignment = new TeacherAssignment(teacher.getId(), discipline.getId(), targetGroup.getId(),
				semester.getId());
		assignment.setStatus(status);
		teacherAssignments.saveAndFlush(assignment);
	}

	private void verifyRequest(String email) throws Exception {
		MvcResult created = mockMvc.perform(post("/api/auth/registration-requests")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
						"fullName", APPLICANT_NAME,
						"email", email,
						"password", "passw0rd1",
						"facultyId", faculty.getId(),
						"groupId", group.getId()))))
				.andExpect(status().isCreated())
				.andReturn();
		String requestId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendVerificationCode(eq(email), any(), code.capture(), any(Duration.class));

		mockMvc.perform(post("/api/auth/registration-requests/{id}/email-verify", requestId)
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("code", code.getValue()))))
				.andExpect(status().isOk());
	}
}
