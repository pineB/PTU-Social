package com.ptu.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ptu.backend.domain.SemesterAcademicRating;
import com.ptu.backend.domain.StudentGroup;
import com.ptu.backend.domain.Submission;
import com.ptu.backend.domain.SubmissionStatus;
import com.ptu.backend.domain.Task;
import com.ptu.backend.domain.TaskStatus;
import com.ptu.backend.repository.SemesterAcademicRatingRepository;
import com.ptu.backend.repository.SubmissionRepository;
import com.ptu.backend.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SemesterArchiveServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SemesterAcademicRatingRepository semesterAcademicRatingRepository;

    @InjectMocks
    private SemesterArchiveService semesterArchiveService;

    @Test
    void shouldPersistSemesterAcademicRatingSnapshotForApprovedSubmissions() {
        StudentGroup group = new StudentGroup();
        group.setId(15L);

        Task task = new Task();
        task.setId(7L);
        task.setGroup(group);
        task.setStatus(TaskStatus.ACTIVE);

        Submission submission = new Submission();
        submission.setId(4L);
        submission.setTask(task);
        submission.setStudentId(11L);
        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setAwardedRating(76);
        submission.setSubmittedAt(LocalDateTime.now().minusDays(1));
        submission.setArchived(false);

        when(taskRepository.findByGroupIdAndStatus(15L, TaskStatus.ACTIVE)).thenReturn(List.of(task));
        when(submissionRepository.findByTaskId(7L)).thenReturn(List.of(submission));
        when(semesterAcademicRatingRepository.save(any(SemesterAcademicRating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        semesterArchiveService.archiveSemester(15L);

        assertEquals(TaskStatus.ARCHIVED, task.getStatus());
        assertTrue(submission.getArchived());

        ArgumentCaptor<SemesterAcademicRating> captor = ArgumentCaptor.forClass(SemesterAcademicRating.class);
        verify(semesterAcademicRatingRepository).save(captor.capture());
        assertEquals(11L, captor.getValue().getStudentId());
        assertEquals(76, captor.getValue().getTotalRating());
    }
}
