package com.ptu.backend.academic.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ptu.backend.academic.domain.AssignmentStatus;
import com.ptu.backend.academic.domain.Discipline;
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
import com.ptu.backend.academic.web.AcademicManagementController.CreateGroupRequest;
import com.ptu.backend.academic.web.AcademicManagementController.ManagedAssignment;
import com.ptu.backend.academic.web.AcademicManagementController.ManagedGroup;
import com.ptu.backend.academic.web.AcademicManagementController.ManagedTeacher;
import com.ptu.backend.academic.web.AcademicManagementController.RenameGroupRequest;
import com.ptu.backend.academic.web.AcademicManagementController.SetActiveRequest;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
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

	public AcademicManagementService(DepartmentRepository departments, DisciplineRepository disciplines,
			FacultyRepository faculties, SemesterRepository semesters, StudyGroupRepository groups,
			TeacherAssignmentRepository assignments, UserRepository users) {
		this.departments = departments;
		this.disciplines = disciplines;
		this.faculties = faculties;
		this.semesters = semesters;
		this.groups = groups;
		this.assignments = assignments;
		this.users = users;
	}

	@Transactional(readOnly = true)
	public List<CatalogItem> departments() {
		return departments.findAll(Sort.by("name")).stream()
				.map(item -> new CatalogItem(item.getId(), item.getName())).toList();
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
