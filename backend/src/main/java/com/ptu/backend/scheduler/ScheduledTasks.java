package com.ptu.backend.scheduler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.ptu.backend.config.AppProperties;
import com.ptu.backend.wallet.service.WalletService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/** Background jobs for persisted reviews, invitations and assignments. */
@Slf4j
@Component
public class ScheduledTasks {

	private final JdbcTemplate jdbc;
	private final WalletService wallets;
	private final AppProperties properties;

	public ScheduledTasks(JdbcTemplate jdbc, WalletService wallets, AppProperties properties) {
		this.jdbc = jdbc;
		this.wallets = wallets;
		this.properties = properties;
	}

	@Scheduled(cron = "0 0 * * * *")
	@Transactional
	public void processApprovedReviews() {
		BigDecimal reward = properties.reviews().rewardAmount();
		int monthlyLimit = properties.reviews().monthlyLimit();
		Instant monthStart = LocalDate.now(ZoneId.systemDefault())
				.withDayOfMonth(1)
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant();

		List<ReviewRow> rows = jdbc.query(
				"select id, author_id from reviews where status = 'APPROVED' and reward_awarded = false "
						+ "order by created_at",
				(rs, rowNum) -> new ReviewRow(rs.getLong("id"), rs.getObject("author_id", UUID.class)));

		int awarded = 0;
		for (ReviewRow row : rows) {
			long awardedThisMonth = wallets.countCreditsSince(
					row.authorId(), WalletService.TYPE_REVIEW_REWARD, monthStart);
			if (awardedThisMonth >= monthlyLimit) {
				log.warn("Monthly review reward limit ({}) reached for user {}, review {} is not rewarded",
						monthlyLimit, row.authorId(), row.id());
				continue;
			}
			wallets.credit(row.authorId(), reward, WalletService.TYPE_REVIEW_REWARD,
					"review-reward:" + row.id());
			jdbc.update("update reviews set reward_awarded = true, updated_at = now() where id = ?", row.id());
			awarded++;
		}

		if (awarded > 0) {
			log.info("Rewarded {} approved reviews", awarded);
		}
	}

	@Scheduled(cron = "0 0 * * * *")
	@Transactional
	public void processOverdueTasks() {
		int expiredInvites = jdbc.update(
				"update teacher_invites set status = 'EXPIRED' "
						+ "where status = 'PENDING' and expires_at < now()");
		if (expiredInvites > 0) {
			log.info("Expired {} teacher invites", expiredInvites);
		}
	}

	record ReviewRow(long id, UUID authorId) {
	}
}
