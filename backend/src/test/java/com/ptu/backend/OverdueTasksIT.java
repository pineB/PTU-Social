package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.ptu.backend.academic.domain.Department;
import com.ptu.backend.academic.repo.DepartmentRepository;
import com.ptu.backend.scheduler.ScheduledTasks;
import com.ptu.backend.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class OverdueTasksIT extends AbstractIntegrationTest {

	@Autowired
	private ScheduledTasks scheduledTasks;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private DepartmentRepository departments;

	@Test
	void expiredPendingInvitesBecomeExpired() {
		UUID departmentId = departments.saveAndFlush(new Department("Кафедра " + UUID.randomUUID())).getId();
		UUID expired = insertInvite(departmentId, "PENDING", Instant.now().minus(1, ChronoUnit.DAYS));
		UUID future = insertInvite(departmentId, "PENDING", Instant.now().plus(1, ChronoUnit.DAYS));
		UUID accepted = insertInvite(departmentId, "ACCEPTED", Instant.now().minus(1, ChronoUnit.DAYS));

		scheduledTasks.processOverdueTasks();

		assertThat(status(expired)).isEqualTo("EXPIRED");
		assertThat(status(future)).isEqualTo("PENDING");
		assertThat(status(accepted)).isEqualTo("ACCEPTED");
	}

	private UUID insertInvite(UUID departmentId, String status, Instant expiresAt) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into teacher_invites (id, email, full_name, department_id, token_hash, status, expires_at) "
				+ "values (?, ?, 'Преподаватель', ?, ?, ?, ?)",
				id, uniqueEmail("invite"), departmentId, "hash-" + id, status, java.sql.Timestamp.from(expiresAt));
		return id;
	}

	private String status(UUID inviteId) {
		return jdbc.queryForObject("select status from teacher_invites where id = ?", String.class, inviteId);
	}
}
