package com.ptu.backend.service;

import com.ptu.backend.domain.AcademicRatingHistory;
import com.ptu.backend.domain.Submission;
import com.ptu.backend.domain.SubmissionStatus;
import com.ptu.backend.domain.Task;
import com.ptu.backend.domain.TaskStatus;
import com.ptu.backend.repository.AcademicRatingHistoryRepository;
import com.ptu.backend.repository.SubmissionRepository;
import com.ptu.backend.repository.TaskRepository;
import com.ptu.backend.web.dto.ReviewSubmissionRequest;
import com.ptu.backend.web.dto.SubmitTaskRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final AcademicRatingHistoryRepository historyRepository;
    private final AcademicRatingService academicRatingService;

    @Transactional
    public Submission submitTask(Long taskId, Long studentId, SubmitTaskRequest request) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.getStatus() == TaskStatus.ARCHIVED) {
            throw new IllegalStateException("Task is archived and cannot receive new submissions");
        }

        Optional<Submission> previous = submissionRepository
            .findFirstByTaskIdAndStudentIdOrderBySubmittedAtDesc(taskId, studentId);

        if (previous.isPresent() && previous.get().getStatus() == SubmissionStatus.PENDING) {
            throw new IllegalStateException("Student already has a pending submission for this task");
        }

        if (previous.isPresent() && previous.get().getStatus() == SubmissionStatus.APPROVED) {
            throw new IllegalStateException("Submission is already approved and cannot be edited");
        }

        Submission submission = new Submission();
        submission.setTask(task);
        submission.setStudentId(studentId);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setContent(request.content());
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setArchived(false);
        submission.setRevisionCount(previous.map(Submission::getRevisionCount).orElse(0));
        submission.setVersionNumber(previous.map(Submission::getVersionNumber).map(v -> v + 1).orElse(1));
        return submissionRepository.save(submission);
    }

    @Transactional
    public Submission reviewSubmission(Long submissionId, ReviewSubmissionRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("Submission not found"));

        if (request.status() == SubmissionStatus.REWORK) {
            submission.setStatus(SubmissionStatus.REWORK);
            submission.setTeacherFeedback(request.feedback());
            submission.setRevisionCount(submission.getRevisionCount() + 1);
            submission.setVersionNumber(submission.getVersionNumber() + 1);
            return submissionRepository.save(submission);
        }

        if (request.status() == SubmissionStatus.APPROVED) {
            submission.setStatus(SubmissionStatus.APPROVED);
            submission.setTeacherFeedback(request.feedback());
            submission.setArchived(false);

            int base = request.baseAcademicRating();
            int daysLate = calculateLateDays(submission.getTask(), submission.getSubmittedAt());
            int rating = academicRatingService.calculateAcademicRating(base, daysLate);

            if (historyRepository.findByStudentIdAndTaskId(submission.getStudentId(), submission.getTask().getId()).isEmpty()) {
                AcademicRatingHistory history = new AcademicRatingHistory();
                history.setStudentId(submission.getStudentId());
                history.setTaskId(submission.getTask().getId());
                history.setRating(rating);
                history.setReason("Approved submission for task " + submission.getTask().getTitle());
                historyRepository.save(history);
            }

            submission.setAwardedRating(rating);
            return submissionRepository.save(submission);
        }

        if (request.status() == SubmissionStatus.ARCHIVED) {
            submission.setStatus(SubmissionStatus.ARCHIVED);
            submission.setTeacherFeedback(request.feedback());
            submission.setArchived(true);
            return submissionRepository.save(submission);
        }

        submission.setStatus(request.status());
        submission.setTeacherFeedback(request.feedback());
        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public List<Submission> getSubmissionsForTask(Long taskId) {
        return submissionRepository.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public Optional<Submission> getLatestSubmission(Long taskId, Long studentId) {
        return submissionRepository.findFirstByTaskIdAndStudentIdOrderBySubmittedAtDesc(taskId, studentId);
    }

    private int calculateLateDays(Task task, LocalDateTime submittedAt) {
        if (!submittedAt.isAfter(task.getDueDate())) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(task.getDueDate().toLocalDate(), submittedAt.toLocalDate());
    }
}
