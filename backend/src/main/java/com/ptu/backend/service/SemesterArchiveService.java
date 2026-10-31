package com.ptu.backend.service;

import com.ptu.backend.domain.SemesterAcademicRating;
import com.ptu.backend.domain.Submission;
import com.ptu.backend.domain.SubmissionStatus;
import com.ptu.backend.domain.Task;
import com.ptu.backend.domain.TaskStatus;
import com.ptu.backend.repository.SemesterAcademicRatingRepository;
import com.ptu.backend.repository.SubmissionRepository;
import com.ptu.backend.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SemesterArchiveService {

    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final SemesterAcademicRatingRepository semesterAcademicRatingRepository;

    @Transactional
    public void archiveSemester(Long groupId) {
        List<Task> tasks = taskRepository.findByGroupIdAndStatus(groupId, TaskStatus.ACTIVE);
        Map<Long, Integer> totalsByStudent = new HashMap<>();

        for (Task task : tasks) {
            task.setStatus(TaskStatus.ARCHIVED);
            taskRepository.save(task);

            List<Submission> submissions = submissionRepository.findByTaskId(task.getId());
            for (Submission submission : submissions) {
                submission.setArchived(true);
                if (submission.getStatus() == SubmissionStatus.APPROVED) {
                    int awarded = submission.getAwardedRating() == null ? 0 : submission.getAwardedRating();
                    totalsByStudent.merge(submission.getStudentId(), awarded, Integer::sum);
                }
                submissionRepository.save(submission);
            }
        }

        if (totalsByStudent.isEmpty()) {
            return;
        }

        String semesterLabel = "SEMESTER-" + LocalDate.now().getYear() + "-" + (LocalDate.now().getMonthValue() <= 6 ? "H1" : "H2");
        for (Map.Entry<Long, Integer> entry : totalsByStudent.entrySet()) {
            SemesterAcademicRating snapshot = new SemesterAcademicRating();
            snapshot.setStudentId(entry.getKey());
            snapshot.setGroupId(groupId);
            snapshot.setTotalRating(entry.getValue());
            snapshot.setArchivedAt(LocalDateTime.now());
            snapshot.setSemesterLabel(semesterLabel);
            semesterAcademicRatingRepository.save(snapshot);
        }
    }
}
