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

/**
 * Background jobs. The {@code reviews} table is owned by the activities/reviews module
 * (not implemented yet), so it is accessed through plain SQL and the job is a no-op
 * while the table does not exist.
 */
@Slf4j
@Component
public class ScheduledTasks {

	static final String REVIEWS_TABLE = "public.reviews";

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
		if (!tableExists(REVIEWS_TABLE)) {
			log.debug("Table {} is absent, skipping review rewards", REVIEWS_TABLE);
			return;
		}

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

	@Scheduled(cron = "0 0 1 1 * *")
	public void processMonthlyConversion() {
		log.info("Monthly currency conversion started: conversion rules are not implemented yet");
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

	private boolean tableExists(String table) {
		Boolean exists = jdbc.queryForObject("select to_regclass(?::text) is not null", Boolean.class, table);
		return Boolean.TRUE.equals(exists);
	}

	record ReviewRow(long id, UUID authorId) {
	}
}
