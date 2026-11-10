package com.ptu.backend.workspace;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ptu.backend.academic.domain.AssignmentStatus;
import com.ptu.backend.academic.domain.Discipline;
import com.ptu.backend.academic.domain.Semester;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.repo.DisciplineRepository;
import com.ptu.backend.academic.repo.SemesterRepository;
import com.ptu.backend.academic.repo.StudyGroupRepository;
import com.ptu.backend.academic.repo.TeacherAssignmentRepository;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.security.UserPrincipal;
import com.ptu.backend.service.AcademicRatingService;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.StudentProfile;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.StudentProfileRepository;
import com.ptu.backend.user.repo.UserRepository;
import com.ptu.backend.workspace.WorkspaceTaskDtos.CreateTaskRequest;
import com.ptu.backend.workspace.WorkspaceTaskDtos.ReviewRequest;
import com.ptu.backend.workspace.WorkspaceTaskDtos.SubmissionResponse;
import com.ptu.backend.workspace.WorkspaceTaskDtos.TaskResponse;
import com.ptu.backend.workspace.WorkspaceTaskDtos.TeachingAssignmentOption;

@Service
public class WorkspaceTaskService {

	private static final long MAX_ATTACHMENT_SIZE = 20L * 1024 * 1024;
	private static final String ATTACHMENT_URL_TEMPLATE = "/api/workspace/submissions/%s/attachment";
	private static final String TASK_ATTACHMENT_URL_TEMPLATE = "/api/workspace/tasks/%s/attachment";

	private final WorkspaceTaskRepository tasks;
	private final WorkspaceSubmissionRepository submissions;
	private final StudentProfileRepository studentProfiles;
	private final UserRepository users;
	private final StudyGroupRepository groups;
	private final DisciplineRepository disciplines;
	private final SemesterRepository semesters;
	private final TeacherAssignmentRepository teacherAssignments;
	private final AcademicRatingService academicRatingService;

	public WorkspaceTaskService(WorkspaceTaskRepository tasks, WorkspaceSubmissionRepository submissions,
			StudentProfileRepository studentProfiles, UserRepository users, StudyGroupRepository groups,
			DisciplineRepository disciplines, SemesterRepository semesters,
			TeacherAssignmentRepository teacherAssignments, AcademicRatingService academicRatingService) {
		this.tasks = tasks;
		this.submissions = submissions;
		this.studentProfiles = studentProfiles;
		this.users = users;
		this.groups = groups;
		this.disciplines = disciplines;
		this.semesters = semesters;
		this.teacherAssignments = teacherAssignments;
		this.academicRatingService = academicRatingService;
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> listStudentTasks(UserPrincipal principal) {
		StudentProfile profile = studentProfile(principal.id());
		return tasks.findAllByGroupIdAndArchivedFalseOrderByDueAtAsc(profile.getGroupId()).stream()
				.map(task -> toTaskResponse(task, submissions.findByTaskIdAndStudentId(task.getId(), principal.id())
						.orElse(null)))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SubmissionResponse> listTeacherSubmissions(UserPrincipal principal) {
		List<WorkspaceTask> teacherTasks = tasks.findAllByTeacherIdAndArchivedFalseOrderByDueAtAsc(principal.id());
		if (teacherTasks.isEmpty()) return List.of();

		return submissions.findAllByTaskIdIn(teacherTasks.stream().map(WorkspaceTask::getId).toList())
				.stream()
				.map(this::toSubmissionResponse)
				.sorted((left, right) -> right.submittedAt().compareTo(left.submittedAt()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> listTeacherTasks(UserPrincipal principal) {
		return tasks.findAllByTeacherIdAndArchivedFalseOrderByDueAtAsc(principal.id()).stream()
				.map(task -> toTaskResponse(task, null))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TeachingAssignmentOption> listTeachingAssignments(UserPrincipal principal) {
		Semester semester = currentSemester();
		return teacherAssignments.findAllByTeacherIdAndSemesterId(principal.id(), semester.getId()).stream()
				.filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
				.map(assignment -> {
					StudyGroup group = groups.findById(assignment.getGroupId()).orElse(null);
					Discipline discipline = disciplines.findById(assignment.getDisciplineId()).orElse(null);
					return group == null || discipline == null ? null
							: new TeachingAssignmentOption(group.getId(), group.getName(),
									discipline.getId(), discipline.getName());
				})
				.filter(option -> option != null)
				.distinct()
				.toList();
	}

	@Transactional
	public TaskResponse createTask(UserPrincipal principal, CreateTaskRequest request, MultipartFile attachment) {
		requireRole(principal, Role.TEACHER);
		Semester semester = currentSemester();
		if (!teacherAssignments.existsByTeacherIdAndGroupIdAndDisciplineIdAndSemesterIdAndStatus(
				principal.id(), request.groupId(), request.disciplineId(), semester.getId(), AssignmentStatus.ACTIVE)) {
			throw ApiException.forbidden("Создавать задания можно только для назначенной группы и дисциплины.");
		}

		StudyGroup group = groups.findById(request.groupId())
				.orElseThrow(() -> ApiException.notFound("Учебная группа не найдена."));
		Discipline discipline = disciplines.findById(request.disciplineId())
				.orElseThrow(() -> ApiException.notFound("Дисциплина не найдена."));
		if (request.dueAt().isBefore(Instant.now())) {
			throw ApiException.badRequest("Срок сдачи должен быть в будущем.");
		}

		WorkspaceTask task = new WorkspaceTask();
		task.setTeacherId(principal.id());
		task.setGroupId(group.getId());
		task.setDisciplineId(discipline.getId());
		task.setTitle(request.title().trim());
		task.setDescription(request.description().trim());
		task.setDueAt(request.dueAt());
		task.setMaxPoints(request.maxPoints());
		task.setAllowLateSubmission(request.allowLateSubmission());
		if (attachment != null && !attachment.isEmpty()) {
			validateAttachment(attachment);
			try {
				task.setAttachmentData(attachment.getBytes());
			} catch (IOException exception) {
				throw ApiException.badRequest("Не удалось прочитать прикреплённый файл.");
			}
			task.setAttachmentName(attachment.getOriginalFilename());
			task.setAttachmentContentType(attachment.getContentType());
		}
		return toTaskResponse(tasks.save(task), null);
	}

	@Transactional
	public TaskResponse submit(UserPrincipal principal, UUID taskId, String content, MultipartFile attachment) {
		requireRole(principal, Role.STUDENT);
		StudentProfile profile = studentProfile(principal.id());
		WorkspaceTask task = tasks.findById(taskId)
				.orElseThrow(() -> ApiException.notFound("Задание не найдено."));
		if (task.isArchived() || !task.getGroupId().equals(profile.getGroupId())) {
			throw ApiException.notFound("Задание не найдено.");
		}
		if (Instant.now().isAfter(task.getDueAt()) && !task.isAllowLateSubmission()) {
			throw ApiException.badRequest("Срок сдачи задания истёк.");
		}

		String trimmedContent = content == null ? "" : content.trim();
		boolean hasAttachment = attachment != null && !attachment.isEmpty();
		if (trimmedContent.isEmpty() && !hasAttachment) {
			throw ApiException.badRequest("Добавьте текст работы или файл.");
		}
		validateAttachment(attachment);

		WorkspaceSubmission submission = submissions.findByTaskIdAndStudentIdForUpdate(taskId, principal.id())
				.orElseGet(() -> {
					WorkspaceSubmission created = new WorkspaceSubmission();
					created.setTaskId(taskId);
					created.setStudentId(principal.id());
					created.setRevisionCount(0);
					return created;
				});
		if (submission.getStatus() == WorkspaceSubmissionStatus.SUBMITTED
				|| submission.getStatus() == WorkspaceSubmissionStatus.ACCEPTED) {
			throw ApiException.conflict("Эта работа уже отправлена или проверена.");
		}
		submission.setStatus(WorkspaceSubmissionStatus.SUBMITTED);
		submission.setSubmittedAt(Instant.now());
		submission.setContent(trimmedContent);
		submission.setTeacherFeedback(null);
		submission.setGrade(null);
		submission.setAwardedRating(null);
		submission.setRevisionCount(submission.getRevisionCount() + 1);
		if (hasAttachment) {
			try {
				submission.setAttachmentData(attachment.getBytes());
			} catch (IOException exception) {
				throw ApiException.badRequest("Не удалось прочитать прикреплённый файл.");
			}
			submission.setAttachmentName(attachment.getOriginalFilename());
			submission.setAttachmentContentType(attachment.getContentType());
		}
		WorkspaceSubmission saved = submissions.save(submission);
		return toTaskResponse(task, saved);
	}

	@Transactional
	public SubmissionResponse review(UserPrincipal principal, UUID submissionId, ReviewRequest request) {
		requireRole(principal, Role.TEACHER);
		WorkspaceSubmission submission = submissions.findByIdForUpdate(submissionId)
				.orElseThrow(() -> ApiException.notFound("Сдача не найдена."));
		WorkspaceTask task = tasks.findById(submission.getTaskId())
				.orElseThrow(() -> ApiException.notFound("Задание не найдено."));
		if (!task.getTeacherId().equals(principal.id())) {
			throw ApiException.forbidden("Вы не можете проверять эту работу.");
		}
		if (submission.getStatus() != WorkspaceSubmissionStatus.SUBMITTED) {
			throw ApiException.conflict("Эта работа уже обработана.");
		}
		if (request.status() == WorkspaceSubmissionStatus.SUBMITTED) {
			throw ApiException.badRequest("Выберите решение: вернуть работу или принять её.");
		}
		if (request.status() == WorkspaceSubmissionStatus.RETURNED) {
			if (request.feedback() == null || request.feedback().isBlank()) {
				throw ApiException.badRequest("Укажите комментарий с замечаниями.");
			}
			submission.setStatus(WorkspaceSubmissionStatus.RETURNED);
			submission.setTeacherFeedback(request.feedback().trim());
		} else {
			if (request.grade() == null) {
				throw ApiException.badRequest("Для принятия работы укажите оценку.");
			}
			submission.setStatus(WorkspaceSubmissionStatus.ACCEPTED);
			submission.setGrade(request.grade());
			submission.setTeacherFeedback(request.feedback() == null ? null : request.feedback().trim());
			int basePoints = (int) Math.floor(task.getMaxPoints() * gradeMultiplier(request.grade()));
			long lateSeconds = Math.max(0, ChronoUnit.SECONDS.between(task.getDueAt(), submission.getSubmittedAt()));
			int lateDays = (int) Math.ceil(lateSeconds / 86_400.0);
			int awardedPoints = academicRatingService.calculateAcademicRating(basePoints, lateDays);
			submission.setAwardedRating(awardedPoints);
			submission.setAwardedAt(Instant.now());
			StudentProfile profile = studentProfiles.findByUserIdForUpdate(submission.getStudentId())
					.orElseThrow(() -> ApiException.notFound("Профиль студента не найден."));
			profile.setAcademicRating(profile.getAcademicRating() + awardedPoints);
			studentProfiles.save(profile);
		}
		return toSubmissionResponse(submissions.save(submission));
	}

	@Transactional(readOnly = true)
	public Attachment attachment(UserPrincipal principal, UUID submissionId) {
		if (principal.role() != Role.STUDENT && principal.role() != Role.TEACHER) {
			throw ApiException.forbidden("Нет доступа к этому вложению.");
		}
		WorkspaceSubmission submission = submissions.findById(submissionId)
				.orElseThrow(() -> ApiException.notFound("Вложение не найдено."));
		if (principal.role() == Role.STUDENT && !submission.getStudentId().equals(principal.id())) {
			throw ApiException.forbidden("Нет доступа к этому вложению.");
		}
		if (principal.role() == Role.TEACHER) {
			WorkspaceTask task = tasks.findById(submission.getTaskId())
					.orElseThrow(() -> ApiException.notFound("Задание не найдено."));
			if (!task.getTeacherId().equals(principal.id())) {
				throw ApiException.forbidden("Нет доступа к этому вложению.");
			}
		}
		if (submission.getAttachmentData() == null || submission.getAttachmentName() == null) {
			throw ApiException.notFound("Вложение не найдено.");
		}
		return new Attachment(submission.getAttachmentName(), submission.getAttachmentContentType(),
				submission.getAttachmentData());
	}

	@Transactional(readOnly = true)
	public Attachment taskAttachment(UserPrincipal principal, UUID taskId) {
		if (principal.role() != Role.STUDENT && principal.role() != Role.TEACHER) {
			throw ApiException.forbidden("Нет доступа к этому вложению.");
		}
		WorkspaceTask task = tasks.findById(taskId).orElseThrow(() -> ApiException.notFound("Задание не найдено."));
		if (principal.role() == Role.TEACHER && !task.getTeacherId().equals(principal.id())) {
			throw ApiException.forbidden("Нет доступа к этому вложению.");
		}
		if (principal.role() == Role.STUDENT && !task.getGroupId().equals(studentProfile(principal.id()).getGroupId())) {
			throw ApiException.forbidden("Нет доступа к этому вложению.");
		}
		if (task.getAttachmentData() == null || task.getAttachmentName() == null) {
			throw ApiException.notFound("Файл к заданию не найден.");
		}
		return new Attachment(task.getAttachmentName(), task.getAttachmentContentType(), task.getAttachmentData());
	}

	private TaskResponse toTaskResponse(WorkspaceTask task, WorkspaceSubmission submission) {
		User teacher = users.findById(task.getTeacherId())
				.orElseThrow(() -> ApiException.notFound("Преподаватель не найден."));
		String status = "in_progress";
		if (submission != null) {
			status = switch (submission.getStatus()) {
				case SUBMITTED -> "submitted";
				case ACCEPTED -> "accepted";
				case RETURNED -> "in_progress";
			};
		} else if (Instant.now().isAfter(task.getDueAt())) {
			status = "overdue";
		}
		String attachmentUrl = submission != null && submission.getAttachmentData() != null
				? ATTACHMENT_URL_TEMPLATE.formatted(submission.getId()) : null;
		String taskAttachmentUrl = task.getAttachmentData() == null
				? null : TASK_ATTACHMENT_URL_TEMPLATE.formatted(task.getId());
		return new TaskResponse(task.getId(), task.getTitle(),
				disciplines.findById(task.getDisciplineId()).map(Discipline::getName).orElse(""),
				teacher.getFullName(), task.getDueAt(), task.getMaxPoints(), task.getDescription(),
				task.isAllowLateSubmission(), status, submission == null ? null : submission.getSubmittedAt(),
				submission == null ? null : submission.getContent(),
				submission == null ? null : submission.getAttachmentName(),
				submission == null ? null : submission.getTeacherFeedback(),
				submission == null ? null : submission.getGrade(),
				submission == null ? null : submission.getAwardedRating(), attachmentUrl,
				task.getAttachmentName(), taskAttachmentUrl);
	}

	private SubmissionResponse toSubmissionResponse(WorkspaceSubmission submission) {
		WorkspaceTask task = tasks.findById(submission.getTaskId())
				.orElseThrow(() -> ApiException.notFound("Задание не найдено."));
		User student = users.findById(submission.getStudentId())
				.orElseThrow(() -> ApiException.notFound("Студент не найден."));
		StudentProfile profile = studentProfile(submission.getStudentId());
		String groupName = groups.findById(profile.getGroupId()).map(StudyGroup::getName).orElse("");
		String attachmentUrl = submission.getAttachmentData() == null ? null
				: ATTACHMENT_URL_TEMPLATE.formatted(submission.getId());
		return new SubmissionResponse(submission.getId(), task.getId(), task.getTitle(),
				disciplines.findById(task.getDisciplineId()).map(Discipline::getName).orElse(""),
				student.getFullName(), groupName, submission.getSubmittedAt(), submission.getContent(),
				submission.getAttachmentName(), submission.getStatus() == WorkspaceSubmissionStatus.RETURNED
						? "in_progress" : submission.getStatus().name().toLowerCase(),
				submission.getTeacherFeedback(), submission.getGrade(), submission.getAwardedRating(), attachmentUrl);
	}

	private StudentProfile studentProfile(UUID userId) {
		return studentProfiles.findByUserId(userId)
				.orElseThrow(() -> ApiException.notFound("Профиль студента не найден."));
	}

	private Semester currentSemester() {
		return semesters.findByCurrentTrue()
				.orElseThrow(() -> ApiException.conflict("Текущий семестр не настроен."));
	}

	private void requireRole(UserPrincipal principal, Role role) {
		if (principal == null || principal.role() != role) {
			throw ApiException.forbidden("Недостаточно прав для этого действия.");
		}
	}

	private void validateAttachment(MultipartFile attachment) {
		if (attachment == null || attachment.isEmpty()) return;
		if (attachment.getSize() > MAX_ATTACHMENT_SIZE) {
			throw ApiException.badRequest("Размер файла не должен превышать 20 МБ.");
		}
		String filename = attachment.getOriginalFilename();
		if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\")) {
			throw ApiException.badRequest("Некорректное имя файла.");
		}
	}

	private double gradeMultiplier(int grade) {
		return switch (grade) {
			case 2 -> 0;
			case 3 -> 0.5;
			case 4 -> 0.75;
			case 5 -> 1;
			default -> throw ApiException.badRequest("Оценка должна быть от 2 до 5.");
		};
	}

	public record Attachment(String fileName, String contentType, byte[] data) {
	}
}
