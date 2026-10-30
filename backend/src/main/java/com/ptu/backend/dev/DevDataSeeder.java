package com.ptu.backend.dev;

import java.time.LocalDate;

import com.ptu.backend.academic.domain.Department;
import com.ptu.backend.academic.domain.Discipline;
import com.ptu.backend.academic.domain.Faculty;
import com.ptu.backend.academic.domain.Semester;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.repo.DepartmentRepository;
import com.ptu.backend.academic.repo.DisciplineRepository;
import com.ptu.backend.academic.repo.FacultyRepository;
import com.ptu.backend.academic.repo.SemesterRepository;
import com.ptu.backend.academic.repo.StudyGroupRepository;
import com.ptu.backend.config.AppProperties;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Idempotent development data: reference data and an academic office account.
 */
@Slf4j
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

	private final FacultyRepository faculties;
	private final DepartmentRepository departments;
	private final StudyGroupRepository groups;
	private final DisciplineRepository disciplines;
	private final SemesterRepository semesters;
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AppProperties properties;

	public DevDataSeeder(FacultyRepository faculties, DepartmentRepository departments, StudyGroupRepository groups,
			DisciplineRepository disciplines, SemesterRepository semesters, UserRepository users,
			PasswordEncoder passwordEncoder, AppProperties properties) {
		this.faculties = faculties;
		this.departments = departments;
		this.groups = groups;
		this.disciplines = disciplines;
		this.semesters = semesters;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		Faculty itFaculty = faculty("Факультет информационных технологий");
		Faculty economicsFaculty = faculty("Факультет экономики");

		Department isDepartment = department("Кафедра информационных систем");
		Department economicsDepartment = department("Кафедра экономики");

		group("ИС-21", itFaculty);
		group("ИС-22", itFaculty);
		group("ЭК-21", economicsFaculty);

		discipline("Программирование", isDepartment);
		discipline("Базы данных", isDepartment);
		discipline("Веб-разработка", isDepartment);
		discipline("Математика", isDepartment);
		discipline("Экономика", economicsDepartment);

		if (semesters.findByCurrentTrue().isEmpty()) {
			semesters.findByName("2026/2027 — осенний").orElseGet(() -> semesters.save(
					new Semester("2026/2027 — осенний", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 31), true)));
		}

		String officeEmail = properties.seed().officeEmail();
		if (!users.existsByEmailIgnoreCase(officeEmail)) {
			User office = new User();
			office.setEmail(officeEmail.toLowerCase());
			office.setFullName("Сотрудник учебной части");
			office.setPasswordHash(passwordEncoder.encode(properties.seed().officePassword()));
			office.setRole(Role.ACADEMIC_OFFICE);
			users.save(office);
			log.info("Dev seeder: created academic office account {}", officeEmail);
		}
		log.info("Dev seeder finished");
	}

	private Faculty faculty(String name) {
		return faculties.findByNameIgnoreCase(name).orElseGet(() -> faculties.save(new Faculty(name)));
	}

	private Department department(String name) {
		return departments.findByNameIgnoreCase(name).orElseGet(() -> departments.save(new Department(name)));
	}

	private StudyGroup group(String name, Faculty faculty) {
		return groups.findByNameIgnoreCase(name)
				.orElseGet(() -> groups.save(new StudyGroup(name, faculty.getId())));
	}

	private Discipline discipline(String name, Department department) {
		return disciplines.findByNameIgnoreCaseAndDepartmentId(name, department.getId())
				.orElseGet(() -> disciplines.save(new Discipline(name, department.getId())));
	}
}
