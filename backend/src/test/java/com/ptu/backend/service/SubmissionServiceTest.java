package com.ptu.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.ptu.backend.domain.AcademicRatingHistory;
import com.ptu.backend.domain.Submission;
import com.ptu.backend.domain.SubmissionStatus;
import com.ptu.backend.domain.Task;
import com.ptu.backend.domain.TaskStatus;
import com.ptu.backend.repository.AcademicRatingHistoryRepository;
import com.ptu.backend.repository.SubmissionRepository;
import com.ptu.backend.repository.TaskRepository;
import com.ptu.backend.web.dto.ReturnForRevisionRequest;
import com.ptu.backend.web.dto.SubmitTaskRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AcademicRatingHistoryRepository historyRepository;

    @Mock
    private AcademicRatingService academicRatingService;

    @InjectMocks
    private SubmissionService submissionService;

    @Test
    void shouldSaveSubmissionTimeAndAttachmentUrl() {
        Task task = new Task();
        task.setId(5L);
        task.setStatus(TaskStatus.ACTIVE);
        task.setDueDate(LocalDateTime.now().plusDays(2));

        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(submissionRepository.findFirstByTaskIdAndStudentIdOrderBySubmittedAtDesc(5L, 10L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission submission = submissionService.submitTask(5L, 10L, new SubmitTaskRequest("answer", "https://example.com/file.pdf"));

        assertNotNull(submission.getSubmittedAt());
        assertEquals("https://example.com/file.pdf", submission.getAttachmentUrl());
        assertEquals(SubmissionStatus.PENDING, submission.getStatus());
    }

    @Test
    void shouldReturnSubmissionForRevisionWithComment() {
        Submission submission = new Submission();
        submission.setId(8L);
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setRevisionCount(0);
        submission.setVersionNumber(1);

        when(submissionRepository.findById(8L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission returned = submissionService.returnForRevision(8L, new ReturnForRevisionRequest("Нужно исправить структуру ответа"));

        assertEquals(SubmissionStatus.REWORK, returned.getStatus());
        assertEquals("Нужно исправить структуру ответа", returned.getTeacherFeedback());
        assertEquals(1, returned.getRevisionCount());
        assertEquals(2, returned.getVersionNumber());
        verify(submissionRepository).save(returned);
    }

    @Test
    void shouldNotAwardAcademicRatingTwiceForApprovedSubmission() {
        Submission submission = new Submission();
        submission.setId(12L);
        submission.setStudentId(10L);
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setAwardedRating(null);

        Task task = new Task();
        task.setId(20L);
        task.setTitle("Final task");
        task.setDueDate(LocalDateTime.now().plusDays(2));
        submission.setTask(task);

        when(submissionRepository.findById(12L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByStudentIdAndTaskId(10L, 20L)).thenReturn(Optional.empty());
        when(academicRatingService.calculateAcademicRating(100, 0)).thenReturn(100);

        Submission firstResult = submissionService.reviewSubmission(12L, new com.ptu.backend.web.dto.ReviewSubmissionRequest(SubmissionStatus.APPROVED, "ok", 100));

        assertEquals(SubmissionStatus.APPROVED, firstResult.getStatus());
        assertEquals(100, firstResult.getAwardedRating());

        submission.setStatus(SubmissionStatus.APPROVED);
        Submission secondResult = submissionService.reviewSubmission(12L, new com.ptu.backend.web.dto.ReviewSubmissionRequest(SubmissionStatus.APPROVED, "ok", 100));

        assertEquals(100, secondResult.getAwardedRating());
        verify(academicRatingService).calculateAcademicRating(100, 0);
    }

    @Test
    void shouldReturnOnlyPendingAndReworkSubmissionsForModerationQueue() {
        Submission pending = new Submission();
        pending.setId(1L);
        pending.setStatus(SubmissionStatus.PENDING);
        pending.setSubmittedAt(LocalDateTime.now().minusHours(2));

        Submission rework = new Submission();
        rework.setId(2L);
        rework.setStatus(SubmissionStatus.REWORK);
        rework.setSubmittedAt(LocalDateTime.now().minusHours(1));

        Submission approved = new Submission();
        approved.setId(3L);
        approved.setStatus(SubmissionStatus.APPROVED);
        approved.setSubmittedAt(LocalDateTime.now());

        when(submissionRepository.findByTask_Group_IdAndStatusInOrderBySubmittedAtAsc(12L, List.of(SubmissionStatus.PENDING, SubmissionStatus.REWORK)))
            .thenReturn(List.of(pending, rework));

        List<Submission> queue = submissionService.getModerationQueueForGroup(12L);

        assertEquals(2, queue.size());
        assertEquals(SubmissionStatus.PENDING, queue.get(0).getStatus());
        assertEquals(SubmissionStatus.REWORK, queue.get(1).getStatus());
    }
}
