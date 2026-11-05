package com.ptu.backend.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select activity from Activity activity where activity.id = :activityId")
	Optional<Activity> findByIdForUpdate(@Param("activityId") Long activityId);
}
