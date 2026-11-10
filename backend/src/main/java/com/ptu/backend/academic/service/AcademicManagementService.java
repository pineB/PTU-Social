package com.ptu.backend.academic.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ptu.backend.academic.domain.AssignmentStatus;
import com.ptu.backend.academic.domain.Department;
import com.ptu.backend.academic.domain.Discipline;
import com.ptu.backend.academic.domain.Faculty;
import com.ptu.backend.academic.domain.Semester;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.domain.TeacherAssignment;
import com.ptu.backend.academic.repo.DepartmentRepository;
import com.ptu.backend.academic.repo.DisciplineRepository;
import com.ptu.backend.academic.repo.FacultyRepository;
import com.ptu.backend.academic.repo.SemesterRepository;
import com.ptu.backend.academic.repo.StudyGroupRepository;
import com.ptu.backend.academic.repo.TeacherAssignmentRepository;
import com.ptu.backend.academic.web.AcademicManagementController.CatalogItem;
import com.ptu.backend.academic.web.AcademicManagementController.CreateAssignmentRequest;
import com.ptu.backend.academic.web.AcademicManagementController.CreateDepartmentRequest;
import com.ptu.backend.academic.web.AcademicManagementController.CreateGroupRequest;
import com.ptu.backend.academic.web.AcademicManagementController.CreateFacultyRequest;
import com.ptu.backend.academic.web.AcademicManagementController.CreateDisciplineRequest;
import com.ptu.backend.academic.web.AcademicManagementController.DisciplineItem;
import com.ptu.backend.academic.web.AcademicManagementController.ManagedAssignment;
import com.ptu.backend.academic.web.AcademicManagementController.ManagedGroup;
import com.ptu.backend.academic.web.AcademicManagementController.ManagedTeacher;
import com.ptu.backend.academic.web.AcademicManagementController.RenameGroupRequest;
import com.ptu.backend.academic.web.AcademicManagementController.RenameDepartmentRequest;
import com.ptu.backend.academic.web.AcademicManagementController.SetActiveRequest;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicManagementService {

	private final DepartmentRepository departments;
	private final DisciplineRepository disciplines;
	private final FacultyRepository faculties;
	private final SemesterRepository semesters;
	private final StudyGroupRepository groups;
	private final TeacherAssignmentRepository assignments;
	private final UserRepository users;
	private final JdbcTemplate jdbc;

	public AcademicManagementService(DepartmentRepository departments, DisciplineRepository disciplines,
			FacultyRepository faculties, SemesterRepository semesters, StudyGroupRepository groups,
			TeacherAssignmentRepository assignments, UserRepository users, JdbcTemplate jdbc) {
		this.departments = departments;
		this.disciplines = disciplines;
		this.faculties = faculties;
		this.semesters = semesters;
		this.groups = groups;
		this.assignments = assignments;
		this.users = users;
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public List<CatalogItem> faculties() {
		return faculties.findAll(Sort.by("name")).stream()
				.map(item -> new CatalogItem(item.getId(), item.getName())).toList();
	}

	@Transactional
	public CatalogItem createFaculty(CreateFacultyRequest request) {
		try {
			Faculty faculty = faculties.saveAndFlush(new Faculty(request.name().trim()));
			return new CatalogItem(faculty.getId(), faculty.getName());
		}
		catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Факультет с таким названием уже существует");
		}
	}

	@Transactional
	public void deleteFaculty(UUID facultyId) {
		Faculty faculty = faculties.findById(facultyId)
				.orElseThrow(() -> ApiException.notFound("Факультет не найден"));
		if (groups.existsByFacultyId(facultyId)) {
			throw ApiException.conflict("Сначала удалите группы факультета");
		}
		faculties.delete(faculty);
	}

	@Transactional(readOnly = true)
	public List<CatalogItem> departments() {
		return departments.findAll(Sort.by("name")).stream()
				.map(item -> new CatalogItem(item.getId(), item.getName())).toList();
	}

	@Transactional
	public CatalogItem createDepartment(CreateDepartmentRequest request) {
		try {
			Department department = departments.saveAndFlush(new Department(request.name().trim()));
			return new CatalogItem(department.getId(), department.getName());
		}
		catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Кафедра с таким названием уже существует");
		}
	}

	@Transactional
	public void deleteDepartment(UUID departmentId) {
		Department department = departments.findById(departmentId)
				.orElseThrow(() -> ApiException.notFound("Кафедра не найдена"));
		Long disciplinesCount = jdbc.queryForObject(
				"select count(*) from disciplines where department_id = ?", Long.class, departmentId);
		if (disciplinesCount != null && disciplinesCount > 0) {
			throw ApiException.conflict("Сначала удалите дисциплины этой кафедры. Используемые в учебном процессе дисциплины удалить нельзя.");
		}
		Long invitesCount = jdbc.queryForObject(
				"select count(*) from teacher_invites where department_id = ?", Long.class, departmentId);
		if (invitesCount != null && invitesCount > 0) {
			throw ApiException.conflict("Нельзя удалить кафедру: с ней связаны приглашения преподавателей.");
		}
		try {
			departments.delete(department);
			departments.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Нельзя удалить кафедру, пока с ней связаны учебные данные.");
		}
	}

	@Transactional
	public CatalogItem renameDepartment(UUID departmentId, RenameDepartmentRequest request) {
		Department department = departments.findById(departmentId)
				.orElseThrow(() -> ApiException.notFound("Кафедра не найдена"));
		department.setName(request.name().trim());
		try {
			Department saved = departments.saveAndFlush(department);
			return new CatalogItem(saved.getId(), saved.getName());
		}
		catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Кафедра с таким названием уже существует");
		}
	}

	@Transactional(readOnly = true)
	public List<DisciplineItem> allDisciplines() {
		Map<UUID, String> departmentNames = departments.findAll().stream()
				.collect(Collectors.toMap(Department::getId, Department::getName));
		return disciplines.findAll(Sort.by("name")).stream()
				.map(discipline -> new DisciplineItem(discipline.getId(), discipline.getName(),
						discipline.getDepartmentId(), departmentNames.getOrDefault(discipline.getDepartmentId(), "")))
				.toList();
	}

	@Transactional
	public DisciplineItem createDiscipline(UUID departmentId, CreateDisciplineRequest request) {
		Department department = departments.findById(departmentId)
				.orElseThrow(() -> ApiException.notFound("Кафедра не найдена"));
		if (disciplines.findByNameIgnoreCaseAndDepartmentId(request.name().trim(), departmentId).isPresent()) {
			throw ApiException.conflict("Такая дисциплина уже есть на этой кафедре");
		}
		Discipline discipline = new Discipline(request.name().trim(), departmentId);
		Discipline saved = disciplines.saveAndFlush(discipline);
		return new DisciplineItem(saved.getId(), saved.getName(), departmentId, department.getName());
	}

	@Transactional(readOnly = true)
	public List<CatalogItem> disciplines(UUID departmentId) {
		if (!departments.existsById(departmentId)) {
			throw ApiException.notFound("Кафедра не найдена");
		}
		return disciplines.findAllByDepartmentId(departmentId, Sort.by("name")).stream()
				.map(item -> new CatalogItem(item.getId(), item.getName())).toList();
	}

	@Transactional(readOnly = true)
	public List<ManagedGroup> groups() {
		Map<UUID, String> facultyNames = faculties.findAll().stream()
				.collect(Collectors.toMap(item -> item.getId(), item -> item.getName()));
		return groups.findAll(Sort.by("name")).stream()
				.map(group -> new ManagedGroup(group.getId(), group.getName(), group.getFacultyId(),
						facultyNames.getOrDefault(group.getFacultyId(), "")))
				.toList();
	}

	@Transactional
	public void deleteDiscipline(UUID disciplineId) {
		Discipline discipline = disciplines.findById(disciplineId)
				.orElseThrow(() -> ApiException.notFound("Дисциплина не найдена"));
		Integer references = jdbc.queryForObject("""
				select
					(select count(*) from teacher_assignments where discipline_id = ?) +
					(select count(*) from teacher_invite_assignments where discipline_id = ?) +
					(select count(*) from workspace_tasks where discipline_id = ?) +
					(select count(*) from campus_schedule_entries where discipline_id = ?)
				""", Integer.class, disciplineId, disciplineId, disciplineId, disciplineId);
		if (references != null && references > 0) {
			throw ApiException.conflict("Нельзя удалить дисциплину, пока она используется в назначениях, заданиях или расписании");
		}
		try {
			disciplines.delete(discipline);
			disciplines.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Нельзя удалить дисциплину, пока она используется в учебном процессе");
		}
	}

	@Transactional
	public ManagedGroup createGroup(CreateGroupRequest request) {
		if (!faculties.existsById(request.facultyId())) {
			throw ApiException.badRequest("Факультет не найден");
		}
		StudyGroup group = new StudyGroup(request.name().trim(), request.facultyId());
		try {
			return toManagedGroup(groups.saveAndFlush(group));
		}
		catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Учебная группа с таким названием уже существует");
		}
	}

	@Transactional
	public ManagedGroup renameGroup(UUID groupId, RenameGroupRequest request) {
		StudyGroup group = groups.findById(groupId).orElseThrow(() -> ApiException.notFound("Группа не найдена"));
		group.setName(request.name().trim());
		try {
			return toManagedGroup(groups.saveAndFlush(group));
		}
		catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Учебная группа с таким названием уже существует");
		}
	}

	@Transactional
	public void deleteGroup(UUID groupId) {
		StudyGroup group = groups.findById(groupId)
				.orElseThrow(() -> ApiException.notFound("Группа не найдена"));
		Integer references = jdbc.queryForObject("""
				select
					(select count(*) from student_profiles where group_id = ?) +
					(select count(*) from teacher_assignments where group_id = ?) +
					(select count(*) from campus_schedule_entries where group_id = ?) +
					(select count(*) from campus_event_group_registrations where group_id = ?) +
					(select count(*) from campus_group_confirmations where group_id = ?) +
					(select count(*) from teacher_invite_assignments where group_id = ?) +
					(select count(*) from registration_requests where group_id = ?)
				""", Integer.class, groupId, groupId, groupId, groupId, groupId, groupId, groupId);
		if (references != null && references > 0) {
			throw ApiException.conflict("Нельзя удалить группу, пока с ней связаны учащиеся, назначения или записи");
		}
		groups.delete(group);
	}

	@Transactional(readOnly = true)
	public List<ManagedTeacher> teachers() {
		List<User> teachers = users.findAllByRole(Role.TEACHER, Sort.by("fullName"));
		if (teachers.isEmpty()) {
			return List.of();
		}
		Map<UUID, String> groupNames = groups.findAll().stream()
				.collect(Collectors.toMap(StudyGroup::getId, StudyGroup::getName));
		Map<UUID, String> disciplineNames = disciplines.findAll().stream()
				.collect(Collectors.toMap(Discipline::getId, Discipline::getName));
		var currentSemester = semesters.findByCurrentTrue();
		return teachers.stream().map(teacher -> {
			List<ManagedAssignment> teacherAssignments = currentSemester
					.map(semester -> assignments.findAllByTeacherIdAndSemesterId(teacher.getId(), semester.getId()))
					.orElseGet(List::of)
					.stream()
					.filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
					.map(assignment -> toManagedAssignment(assignment, groupNames, disciplineNames))
					.toList();
			return new ManagedTeacher(teacher.getId(), teacher.getFullName(), teacher.getEmail(), teacher.isActive(),
					teacherAssignments);
		}).toList();
	}

	@Transactional
	public ManagedTeacher setTeacherActive(UUID teacherId, SetActiveRequest request) {
		User teacher = users.findById(teacherId).orElseThrow(() -> ApiException.notFound("Преподаватель не найден"));
		if (teacher.getRole() != Role.TEACHER) {
			throw ApiException.notFound("Преподаватель не найден");
		}
		teacher.setActive(request.active());
		users.save(teacher);
		return teachers().stream().filter(item -> item.id().equals(teacherId)).findFirst()
				.orElseThrow(() -> ApiException.notFound("Преподаватель не найден"));
	}

	@Transactional
	public ManagedAssignment assignTeacher(UUID teacherId, CreateAssignmentRequest request) {
		User teacher = users.findById(teacherId).orElseThrow(() -> ApiException.notFound("Преподаватель не найден"));
		if (teacher.getRole() != Role.TEACHER) {
			throw ApiException.notFound("Преподаватель не найден");
		}
		if (!teacher.isActive()) {
			throw ApiException.conflict("Нельзя назначить группу неактивному преподавателю");
		}
		StudyGroup group = groups.findById(request.groupId())
				.orElseThrow(() -> ApiException.badRequest("Группа не найдена"));
		Discipline discipline = disciplines.findById(request.disciplineId())
				.orElseThrow(() -> ApiException.badRequest("Дисциплина не найдена"));
		Semester semester = semesters.findByCurrentTrue()
				.orElseThrow(() -> ApiException.conflict("Текущий семестр не настроен"));

		TeacherAssignment assignment = assignments
				.findByTeacherIdAndGroupIdAndDisciplineIdAndSemesterId(teacherId, group.getId(), discipline.getId(),
						semester.getId())
				.orElseGet(() -> new TeacherAssignment(teacherId, discipline.getId(), group.getId(), semester.getId()));
		assignment.setStatus(AssignmentStatus.ACTIVE);
		assignments.save(assignment);
		return new ManagedAssignment(assignment.getId(), group.getId(), group.getName(), discipline.getId(),
				discipline.getName());
	}

	@Transactional
	public void archiveAssignment(UUID teacherId, UUID assignmentId) {
		TeacherAssignment assignment = assignments.findById(assignmentId)
				.orElseThrow(() -> ApiException.notFound("Назначение не найдено"));
		if (!assignment.getTeacherId().equals(teacherId)) {
			throw ApiException.notFound("Назначение не найдено");
		}
		assignment.setStatus(AssignmentStatus.ARCHIVED);
	}

	private ManagedGroup toManagedGroup(StudyGroup group) {
		String facultyName = faculties.findById(group.getFacultyId()).map(item -> item.getName()).orElse("");
		return new ManagedGroup(group.getId(), group.getName(), group.getFacultyId(), facultyName);
	}

	private ManagedAssignment toManagedAssignment(TeacherAssignment assignment, Map<UUID, String> groupNames,
			Map<UUID, String> disciplineNames) {
		return new ManagedAssignment(assignment.getId(), assignment.getGroupId(),
				groupNames.getOrDefault(assignment.getGroupId(), ""),
				assignment.getDisciplineId(), disciplineNames.getOrDefault(assignment.getDisciplineId(), ""));
	}
}
