package com.ptu.backend.repository;

import com.ptu.backend.domain.SemesterAcademicRating;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterAcademicRatingRepository extends JpaRepository<SemesterAcademicRating, Long> {

    List<SemesterAcademicRating> findByGroupId(Long groupId);
}
