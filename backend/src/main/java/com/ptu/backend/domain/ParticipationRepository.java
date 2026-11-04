package com.ptu.backend.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    boolean existsByActivityIdAndStudentId(Long activityId, UUID studentId);
    long countByActivityId(Long activityId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Participation participation " +
            "set participation.status = :confirmed " +
            "where participation.activityId = :activityId and participation.status = :registered")
    int confirmRegisteredByActivityId(@org.springframework.data.repository.query.Param("activityId") Long activityId,
                                      @org.springframework.data.repository.query.Param("registered") ParticipationStatus registered,
                                      @org.springframework.data.repository.query.Param("confirmed") ParticipationStatus confirmed);
}
