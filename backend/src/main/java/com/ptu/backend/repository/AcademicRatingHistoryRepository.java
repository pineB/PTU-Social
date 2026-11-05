package com.ptu.backend.repository;

import com.ptu.backend.domain.AcademicRatingHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicRatingHistoryRepository extends JpaRepository<AcademicRatingHistory, Long> {

    Optional<AcademicRatingHistory> findByStudentIdAndTaskId(Long studentId, Long taskId);
}
