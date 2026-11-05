package com.ptu.backend.bootstrap;

import com.ptu.backend.domain.Department;
import com.ptu.backend.domain.Discipline;
import com.ptu.backend.domain.Faculty;
import com.ptu.backend.domain.StudentGroup;
import com.ptu.backend.domain.Submission;
import com.ptu.backend.domain.SubmissionStatus;
import com.ptu.backend.domain.Task;
import com.ptu.backend.domain.TaskStatus;
import com.ptu.backend.domain.TeacherAssignment;
import com.ptu.backend.repository.DepartmentRepository;
import com.ptu.backend.repository.DisciplineRepository;
import com.ptu.backend.repository.FacultyRepository;
import com.ptu.backend.repository.GroupRepository;
import com.ptu.backend.repository.SubmissionRepository;
import com.ptu.backend.repository.TaskRepository;
import com.ptu.backend.repository.TeacherAssignmentRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final GroupRepository groupRepository;
    private final DisciplineRepository disciplineRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (taskRepository.count() > 0 || submissionRepository.count() > 0) {
            return;
        }

        Faculty faculty = new Faculty();
        faculty.setName("Факультет информационных технологий");
        faculty = facultyRepository.save(faculty);

        Department department = new Department();
        department.setName("Кафедра программной инженерии");
        department.setFaculty(faculty);
        department = departmentRepository.save(department);

        StudentGroup group = new StudentGroup();
        group.setName("ИВТ-101");
        group.setDepartment(department);
        group = groupRepository.save(group);

        Discipline discipline = new Discipline();
        discipline.setName("Java Backend");
        discipline = disciplineRepository.save(discipline);

        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setTeacherId(101L);
        assignment.setGroup(group);
        assignment.setDiscipline(discipline);
        teacherAssignmentRepository.save(assignment);

        Task task = new Task();
        task.setTitle("Демо: CRUD для курса");
        task.setDescription("Реализовать базовый CRUD для учебного модуля");
        task.setDueDate(LocalDateTime.now().plusDays(3));
        task.setTeacherId(101L);
        task.setGroup(group);
        task.setDiscipline(discipline);
        task.setStatus(TaskStatus.ACTIVE);
        task = taskRepository.save(task);

        Submission submission = new Submission();
        submission.setTask(task);
        submission.setStudentId(201L);
        submission.setSubmittedAt(LocalDateTime.now().minusDays(1));
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setContent("Черновой вариант решения");
        submission.setRevisionCount(0);
        submission.setVersionNumber(1);
        submission.setArchived(false);
        submissionRepository.save(submission);

        Task task2 = new Task();
        task2.setTitle("Демо: экзаменационный мини-проект");
        task2.setDescription("Подготовить финальную сборку и показать сценарий сдачи");
        task2.setDueDate(LocalDateTime.now().plusDays(7));
        task2.setTeacherId(101L);
        task2.setGroup(group);
        task2.setDiscipline(discipline);
        task2.setStatus(TaskStatus.ACTIVE);
        task2 = taskRepository.save(task2);

        Submission approved = new Submission();
        approved.setTask(task2);
        approved.setStudentId(202L);
        approved.setSubmittedAt(LocalDateTime.now().minusDays(2));
        approved.setStatus(SubmissionStatus.APPROVED);
        approved.setContent("Готовый проект с подтверждением");
        approved.setAwardedRating(90);
        approved.setRevisionCount(0);
        approved.setVersionNumber(1);
        approved.setArchived(false);
        submissionRepository.save(approved);
    }
}
