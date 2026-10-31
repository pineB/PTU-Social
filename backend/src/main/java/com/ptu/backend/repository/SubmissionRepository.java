package com.ptu.backend.repository;

import com.ptu.backend.domain.Submission;
import com.ptu.backend.domain.SubmissionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByTaskId(Long taskId);

    Optional<Submission> findFirstByTaskIdAndStudentIdOrderBySubmittedAtDesc(Long taskId, Long studentId);

    List<Submission> findByStudentIdAndStatus(Long studentId, SubmissionStatus status);

    List<Submission> findByTask_Group_IdAndStatusInOrderBySubmittedAtAsc(Long groupId, List<SubmissionStatus> statuses);
}
