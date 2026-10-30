package com.ptu.backend.auth.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teacher_invite_assignments")
@Getter
@Setter
@NoArgsConstructor
public class TeacherInviteAssignment {

	@EmbeddedId
	private TeacherInviteAssignmentId id;

	public TeacherInviteAssignment(TeacherInviteAssignmentId id) {
		this.id = id;
	}
}
