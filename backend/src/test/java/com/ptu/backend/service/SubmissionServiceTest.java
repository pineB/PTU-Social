package com.ptu.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
