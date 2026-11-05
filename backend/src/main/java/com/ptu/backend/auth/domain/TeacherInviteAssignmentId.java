package com.ptu.backend.auth.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInviteAssignmentId implements Serializable {

	@Column(name = "invite_id")
	private UUID inviteId;

	@Column(name = "discipline_id")
	private UUID disciplineId;

	@Column(name = "group_id")
	private UUID groupId;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TeacherInviteAssignmentId that)) {
			return false;
		}
		return Objects.equals(inviteId, that.inviteId)
				&& Objects.equals(disciplineId, that.disciplineId)
				&& Objects.equals(groupId, that.groupId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(inviteId, disciplineId, groupId);
	}
}
