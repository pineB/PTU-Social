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
@Table(name = "disciplines")
@Getter
@Setter
@NoArgsConstructor
public class Discipline {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, columnDefinition = "citext")
	private String name;

	@Column(name = "department_id", nullable = false)
	private UUID departmentId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public Discipline(String name, UUID departmentId) {
		this.name = name;
		this.departmentId = departmentId;
	}
}
