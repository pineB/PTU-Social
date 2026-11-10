package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ptu.backend.campus.CampusDtos.EventView;
import com.ptu.backend.campus.CampusService;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CampusEventReviewRatingIT extends AbstractIntegrationTest {

    @Autowired
    private CampusService campusService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void eventRatingIsCalculatedFromApprovedReviewsOnTheServer() {
        User organizer = createUser(uniqueEmail("review-rating-organizer"), "Password123", Role.TEACHER);
        long eventId = insertEvent(organizer.getId());

        for (int i = 0; i < 9; i++) {
            insertReview(eventId, student(i), 5, "APPROVED");
        }
        User pendingAuthor = student(9);
        long pendingReviewId = insertReview(eventId, pendingAuthor.getId(), 1, "PENDING_MODERATION");

        assertThat(event(eventId).rating()).isNull();

        jdbc.update("update reviews set status = 'APPROVED' where id = ?", pendingReviewId);

        assertThat(event(eventId).rating()).isEqualByComparingTo("4.6");
    }

    private EventView event(long eventId) {
        return campusService.events(null).stream()
                .filter(item -> item.id().equals(Long.toString(eventId)))
                .findFirst()
                .orElseThrow();
    }

    private long insertEvent(UUID organizerId) {
        Long id = jdbc.queryForObject("""
                insert into activities (
                    title, description, type, participation_mode, starts_at, ends_at,
                    max_participants, min_participants, required_activation_percent, teacher_id
                ) values (?, '', 'EVENT', 'INDIVIDUAL', ?, ?, 100, 0, 0, ?)
                returning id
                """, Long.class, "Review rating integration event",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), organizerId);
        assertThat(id).isNotNull();
        return id;
    }

    private User student(int index) {
        return createUser(uniqueEmail("review-rating-student-" + index), "Password123", Role.STUDENT);
    }

    private long insertReview(long eventId, User author, int rating, String status) {
        return insertReview(eventId, author.getId(), rating, status);
    }

    private long insertReview(long eventId, UUID authorId, int rating, String status) {
        Long id = jdbc.queryForObject("""
                insert into reviews (activity_id, author_id, text, rating, status)
                values (?, ?, 'A sufficiently detailed event review', ?, ?)
                returning id
                """, Long.class, eventId, authorId, rating, status);
        assertThat(id).isNotNull();
        return id;
    }
}
