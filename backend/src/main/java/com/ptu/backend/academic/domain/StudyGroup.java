package com.ptu.backend.academic.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "study_groups")
@Getter
@Setter
@NoArgsConstructor
public class StudyGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, columnDefinition = "citext")
	private String name;

	@Column(name = "faculty_id", nullable = false)
	private UUID facultyId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public StudyGroup(String name, UUID facultyId) {
		this.name = name;
		this.facultyId = facultyId;
	}
}
