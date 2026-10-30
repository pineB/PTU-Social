package com.ptu.backend.service;

import com.ptu.backend.domain.Submission;
import com.ptu.backend.domain.Task;
import com.ptu.backend.domain.TaskStatus;
import com.ptu.backend.repository.SubmissionRepository;
import com.ptu.backend.repository.TaskRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SemesterArchiveService {

    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional
    public void archiveSemester(Long groupId) {
        List<Task> tasks = taskRepository.findByGroupIdAndStatus(groupId, TaskStatus.ACTIVE);
        for (Task task : tasks) {
            task.setStatus(TaskStatus.ARCHIVED);
            taskRepository.save(task);

            List<Submission> submissions = submissionRepository.findByTaskId(task.getId());
            for (Submission submission : submissions) {
                submission.setArchived(true);
                submissionRepository.save(submission);
            }
        }
    }
}
