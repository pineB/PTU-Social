package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
import com.ptu.backend.auth.domain.RegistrationRequest;
import com.ptu.backend.auth.domain.RegistrationStatus;
import com.ptu.backend.auth.repo.RegistrationRequestRepository;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.StudentProfile;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.StudentProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class ApprovalAccessIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RegistrationRequestRepository registrationRequests;

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

	private Faculty faculty;
	private StudyGroup groupA;
	private StudyGroup groupB;
	private Semester semester;
	private Discipline discipline;

	@BeforeEach
	void setUp() {
		faculty = createFaculty("Факультет " + UUID.randomUUID());
		groupA = createGroup("ИС-21-" + UUID.randomUUID(), faculty);
		groupB = createGroup("ИС-22-" + UUID.randomUUID(), faculty);
		semester = semesters.save(new Semester("2026/2027 " + UUID.randomUUID(), LocalDate.of(2026, 9, 1),
				LocalDate.of(2027, 1, 31), true));
		Department department = departments.save(new Department("Кафедра ИТ " + UUID.randomUUID()));
		discipline = disciplines.save(new Discipline("Сети " + UUID.randomUUID(), department.getId()));
	}

	@Test
	void teacherApprovesOnlyOwnGroups() throws Exception {
		User teacher = createUser(uniqueEmail("teacher"), "teacher12345", Role.TEACHER);
		assignTeacher(teacher, groupA);

		UUID own = verifiedRequest(groupA);
		UUID foreign = verifiedRequest(groupB);

		mockMvc.perform(post("/api/academic/registration-requests/{id}/approve", foreign)
						.header("Authorization", bearer(teacher)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/academic/registration-requests/{id}/approve", own)
						.header("Authorization", bearer(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void teacherListContainsOnlyOwnGroups() throws Exception {
		User teacher = createUser(uniqueEmail("teacher"), "teacher12345", Role.TEACHER);
		assignTeacher(teacher, groupA);
		verifiedRequest(groupA);
		verifiedRequest(groupB);

		mockMvc.perform(get("/api/academic/registration-requests")
						.header("Authorization", bearer(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].groupId").value(groupA.getId().toString()));
	}

	@Test
	void groupLeaderApprovesOwnGroupOnly() throws Exception {
		User leader = createUser(uniqueEmail("leader"), "leader12345", Role.STUDENT);
		StudentProfile profile = new StudentProfile();
		profile.setUserId(leader.getId());
		profile.setGroupId(groupA.getId());
		profile.setGroupLeader(true);
		studentProfiles.saveAndFlush(profile);

		UUID own = verifiedRequest(groupA);
		UUID foreign = verifiedRequest(groupB);

		mockMvc.perform(post("/api/academic/registration-requests/{id}/approve", own)
						.header("Authorization", bearer(leader)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/academic/registration-requests/{id}/approve", foreign)
						.header("Authorization", bearer(leader)))
				.andExpect(status().isForbidden());
	}

	@Test
	void regularStudentCannotApprove() throws Exception {
		User student = createUser(uniqueEmail("student"), "student12345", Role.STUDENT);
		StudentProfile profile = new StudentProfile();
		profile.setUserId(student.getId());
		profile.setGroupId(groupA.getId());
		profile.setGroupLeader(false);
		studentProfiles.saveAndFlush(profile);

		mockMvc.perform(post("/api/academic/registration-requests/{id}/approve", verifiedRequest(groupA))
						.header("Authorization", bearer(student)))
				.andExpect(status().isForbidden());
	}

	@Test
	void academicOfficeApprovesAnyGroupAsFallback() throws Exception {
		User office = createUser(uniqueEmail("office"), "office12345", Role.ACADEMIC_OFFICE);

		mockMvc.perform(post("/api/academic/registration-requests/{id}/approve", verifiedRequest(groupB))
						.header("Authorization", bearer(office)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void rejectedRequestClearsPasswordHash() throws Exception {
		User office = createUser(uniqueEmail("office"), "office12345", Role.ACADEMIC_OFFICE);
		UUID requestId = verifiedRequest(groupA);

		mockMvc.perform(post("/api/academic/registration-requests/{id}/reject", requestId)
						.header("Authorization", bearer(office)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"));

		RegistrationRequest stored = registrationRequests.findById(requestId).orElseThrow();
		assertThat(stored.getPasswordHash()).isNull();
	}

	private void assignTeacher(User teacher, StudyGroup group) {
		teacherAssignments.saveAndFlush(new TeacherAssignment(teacher.getId(), discipline.getId(), group.getId(),
				semester.getId()));
		assertThat(teacherAssignments.findGroupIds(teacher.getId(), semester.getId(), AssignmentStatus.ACTIVE))
				.contains(group.getId());
	}

	private UUID verifiedRequest(StudyGroup group) {
		RegistrationRequest request = new RegistrationRequest();
		request.setEmail(uniqueEmail("applicant"));
		request.setFullName("Студент " + UUID.randomUUID());
		request.setPasswordHash(passwordEncoder.encode("student12345"));
		request.setFacultyId(faculty.getId());
		request.setGroupId(group.getId());
		request.setStatus(RegistrationStatus.EMAIL_VERIFIED);
		request.setEmailVerifiedAt(Instant.now());
		request.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
		return registrationRequests.saveAndFlush(request).getId();
	}
}
