package com.ptu.backend.academic.domain;

import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "semesters")
@Getter
@Setter
@NoArgsConstructor
public class Semester {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "starts_on", nullable = false)
	private LocalDate startsOn;

	@Column(name = "ends_on", nullable = false)
	private LocalDate endsOn;

	@Column(name = "is_current", nullable = false)
	private boolean current;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public Semester(String name, LocalDate startsOn, LocalDate endsOn, boolean current) {
		this.name = name;
		this.startsOn = startsOn;
		this.endsOn = endsOn;
		this.current = current;
	}
}
