package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class EventReviewModerationIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void academicOfficeSeesPendingReviewsWithFlaggedWords() throws Exception {
        User organizer = createUser(uniqueEmail("review-queue-organizer"), "Password123", Role.TEACHER);
        User academic = createUser(uniqueEmail("review-queue-academic"), "Password123", Role.ACADEMIC_OFFICE);
        User student = createUser(uniqueEmail("review-queue-student"), "Password123", Role.STUDENT);
        User otherStudent = createUser(uniqueEmail("review-queue-student-2"), "Password123", Role.STUDENT);
        long eventId = insertEvent(organizer.getId());
        long cleanReviewId = insertReview(eventId, student.getId(), "Отличное мероприятие, всё понравилось");
        long flaggedReviewId = insertReview(eventId, otherStudent.getId(), "Ну хуйня полная, но в целом неплохо");

        String body = mockMvc.perform(get("/api/campus/reviews/moderation")
                        .header("Authorization", bearer(academic)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> allItems = objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {
        });
        List<Map<String, Object>> queue = allItems.stream()
                .filter(item -> Long.toString(eventId).equals(item.get("eventId")))
                .toList();
        assertThat(queue).hasSize(2);
        assertThat(queue).extracting(item -> item.get("id"))
                .containsExactlyInAnyOrder(Long.toString(cleanReviewId), Long.toString(flaggedReviewId));

        Map<String, Object> flagged = queue.stream()
                .filter(item -> Long.toString(flaggedReviewId).equals(item.get("id")))
                .findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        List<String> flaggedWords = (List<String>) flagged.get("flaggedWords");
        assertThat(flaggedWords).contains("хуй");
        assertThat(flagged.get("eventTitle")).isEqualTo("Review moderation event");

        Map<String, Object> clean = queue.stream()
                .filter(item -> Long.toString(cleanReviewId).equals(item.get("id")))
                .findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        List<String> cleanWords = (List<String>) clean.get("flaggedWords");
        assertThat(cleanWords).isEmpty();
    }

    @Test
    void teacherCannotReadQueueOrModerate() throws Exception {
        User organizer = createUser(uniqueEmail("review-rights-organizer"), "Password123", Role.TEACHER);
        User student = createUser(uniqueEmail("review-rights-student"), "Password123", Role.STUDENT);
        long eventId = insertEvent(organizer.getId());
        long reviewId = insertReview(eventId, student.getId(), "Отличное мероприятие, всё понравилось");

        mockMvc.perform(get("/api/campus/reviews/moderation")
                        .header("Authorization", bearer(organizer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/campus/events/{eventId}/reviews/{reviewId}", eventId, reviewId)
                        .header("Authorization", bearer(organizer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isForbidden());

        assertThat(reviewStatus(reviewId)).isEqualTo("PENDING_MODERATION");
    }

    @Test
    void academicOfficeApprovesAndRejectsPendingReviews() throws Exception {
        User organizer = createUser(uniqueEmail("review-decision-organizer"), "Password123", Role.TEACHER);
        User academic = createUser(uniqueEmail("review-decision-academic"), "Password123", Role.ACADEMIC_OFFICE);
        User student = createUser(uniqueEmail("review-decision-student"), "Password123", Role.STUDENT);
        User otherStudent = createUser(uniqueEmail("review-decision-student-2"), "Password123", Role.STUDENT);
        long eventId = insertEvent(organizer.getId());
        long approvedReviewId = insertReview(eventId, student.getId(), "Отличное мероприятие, всё понравилось");
        long rejectedReviewId = insertReview(eventId, otherStudent.getId(), "Ну хуйня полная, но в целом неплохо");

        mockMvc.perform(patch("/api/campus/events/{eventId}/reviews/{reviewId}", eventId, approvedReviewId)
                        .header("Authorization", bearer(academic))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk());
        assertThat(reviewStatus(approvedReviewId)).isEqualTo("APPROVED");
        assertThat(moderatedBy(approvedReviewId)).isEqualTo(academic.getId());

        mockMvc.perform(patch("/api/campus/events/{eventId}/reviews/{reviewId}", eventId, approvedReviewId)
                        .header("Authorization", bearer(academic))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/campus/events/{eventId}/reviews/{reviewId}", eventId, rejectedReviewId)
                        .header("Authorization", bearer(academic))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":false}"))
                .andExpect(status().isOk());
        assertThat(reviewStatus(rejectedReviewId)).isEqualTo("REJECTED");
    }

    private String reviewStatus(long reviewId) {
        return jdbc.queryForObject("select status from reviews where id = ?", String.class, reviewId);
    }

    private UUID moderatedBy(long reviewId) {
        return jdbc.queryForObject("select moderated_by from reviews where id = ?", UUID.class, reviewId);
    }

    private long insertEvent(UUID organizerId) {
        Long id = jdbc.queryForObject("""
                insert into activities (
                    title, description, type, participation_mode, starts_at, ends_at,
                    max_participants, min_participants, required_activation_percent, teacher_id
                ) values (?, '', 'EVENT', 'INDIVIDUAL', ?, ?, 100, 0, 0, ?)
                returning id
                """, Long.class, "Review moderation event",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(1), organizerId);
        assertThat(id).isNotNull();
        return id;
    }

    private long insertReview(long eventId, UUID authorId, String text) {
        Long id = jdbc.queryForObject("""
                insert into reviews (activity_id, author_id, text, rating, status)
                values (?, ?, ?, 5, 'PENDING_MODERATION')
                returning id
                """, Long.class, eventId, authorId, text);
        assertThat(id).isNotNull();
        return id;
    }
}
