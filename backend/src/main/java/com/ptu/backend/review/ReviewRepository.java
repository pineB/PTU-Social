package com.ptu.backend.review;

import com.ptu.backend.domain.ActivityType;
import com.ptu.backend.domain.Review;
import com.ptu.backend.domain.ReviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("select review from Review review, Activity activity " +
            "where activity.id = review.activityId and activity.type = :activityType " +
            "and review.status = :status order by review.createdAt asc")
    Page<Review> findModerationQueue(@Param("activityType") ActivityType activityType,
                                     @Param("status") ReviewStatus status,
                                     Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select review from Review review, Activity activity " +
            "where activity.id = review.activityId and activity.type = :activityType and review.id = :reviewId")
    Optional<Review> findAcademicByIdForUpdate(@Param("reviewId") Long reviewId,
                                              @Param("activityType") ActivityType activityType);
}
