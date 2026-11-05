package com.ptu.backend.repository;

import com.ptu.backend.domain.SubmissionVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionVersionRepository extends JpaRepository<SubmissionVersion, Long> {

    List<SubmissionVersion> findBySubmissionIdOrderByVersionNumberAsc(Long submissionId);
}
