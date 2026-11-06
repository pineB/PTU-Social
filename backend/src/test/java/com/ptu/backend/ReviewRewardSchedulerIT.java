package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import com.ptu.backend.scheduler.ScheduledTasks;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.wallet.repo.TransactionRepository;
import com.ptu.backend.wallet.service.WalletService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ReviewRewardSchedulerIT extends AbstractIntegrationTest {

	@Autowired
	private ScheduledTasks scheduledTasks;

	@Autowired
	private WalletService walletService;

	@Autowired
	private TransactionRepository transactions;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void approvedReviewIsRewardedOnce() {
		User author = student();
		long reviewId = insertReview(author.getId(), "APPROVED", false);

		scheduledTasks.processApprovedReviews();

		assertThat(walletService.balance(author.getId())).isEqualByComparingTo("5");
		assertThat(rewardAwarded(reviewId)).isTrue();
		assertThat(transactions.count()).isEqualTo(1);

		scheduledTasks.processApprovedReviews();

		assertThat(walletService.balance(author.getId())).isEqualByComparingTo("5");
		assertThat(transactions.count()).isEqualTo(1);
	}

	@Test
	void notApprovedReviewIsNotRewarded() {
		User author = student();
		long reviewId = insertReview(author.getId(), "PENDING_MODERATION", false);

		scheduledTasks.processApprovedReviews();

		assertThat(walletService.balance(author.getId())).isEqualByComparingTo("0");
		assertThat(rewardAwarded(reviewId)).isFalse();
	}

	@Test
	void monthlyLimitIsEnforced() {
		User author = student();
		long reviewId = insertReview(author.getId(), "APPROVED", false);

		for (int i = 1; i <= 5; i++) {
			walletService.credit(author.getId(), new BigDecimal("5"),
					WalletService.TYPE_REVIEW_REWARD, "monthly-" + i);
		}

		scheduledTasks.processApprovedReviews();

		assertThat(walletService.balance(author.getId())).isEqualByComparingTo("25");
		assertThat(rewardAwarded(reviewId)).isFalse();
	}

	private User student() {
		return createUser(uniqueEmail("review-author"), "Password123", Role.STUDENT);
	}

	private long insertReview(UUID authorId, String status, boolean rewardAwarded) {
		Long id = jdbc.queryForObject(
				"insert into reviews (activity_id, author_id, text, rating, status, reward_awarded) "
						+ "values (1, ?, 'Содержательный отзыв', 5, ?, ?) returning id",
				Long.class, authorId, status, rewardAwarded);
		assertThat(id).isNotNull();
		return id;
	}

	private boolean rewardAwarded(long reviewId) {
		Boolean awarded = jdbc.queryForObject("select reward_awarded from reviews where id = ?",
				Boolean.class, reviewId);
		return Boolean.TRUE.equals(awarded);
	}
}
