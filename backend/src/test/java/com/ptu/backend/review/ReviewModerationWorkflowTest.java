package com.ptu.backend.review;

import com.ptu.backend.api.dto.ReviewResponse;
import com.ptu.backend.domain.ActivityType;
import com.ptu.backend.domain.Review;
import com.ptu.backend.domain.ReviewStatus;
import com.ptu.backend.review.filter.SpamReviewFilterStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewModerationWorkflowTest {

    @Test
    void repeatedNormalWordsAreNotAutomaticallyClassifiedAsSpam() {
        SpamReviewFilterStrategy filter = new SpamReviewFilterStrategy();

        assertTrue(filter.isValid("The course was useful, and the course helped me understand the topic."));
        assertFalse(filter.isValid("promo promo promo promo promo promo"));
    }

    @Test
    void approvingTheSameReviewTwiceAwardsCurrencyOnlyOnce() {
        ReviewRepository repository = mock(ReviewRepository.class);
        ObjectProvider<ReviewCurrencyAwarder> awarderProvider = mock(ObjectProvider.class);
        ReviewCurrencyAwarder awarder = mock(ReviewCurrencyAwarder.class);
        UUID authorId = UUID.randomUUID();
        Review review = new Review();
        review.setId(5L);
        review.setAuthorId(authorId);
        review.setStatus(ReviewStatus.PENDING_MODERATION);

        when(repository.findAcademicByIdForUpdate(5L, ActivityType.ACADEMIC)).thenReturn(Optional.of(review));
        when(awarderProvider.getIfAvailable()).thenReturn(awarder);

        ReviewService service = new ReviewService(repository, awarderProvider);
        ReviewResponse firstDecision = service.moderateReview(5L, true);
        ReviewResponse repeatedDecision = service.moderateReview(5L, true);

        assertEquals(ReviewStatus.APPROVED, firstDecision.status());
        assertTrue(firstDecision.rewardAwarded());
        assertTrue(repeatedDecision.rewardAwarded());
        verify(awarder, times(1)).awardOnce(5L, authorId);
    }

    @Test
    void moderatorQueueRequestsPendingAcademicReviewsOnly() {
        ReviewRepository repository = mock(ReviewRepository.class);
        ObjectProvider<ReviewCurrencyAwarder> awarderProvider = mock(ObjectProvider.class);
        when(repository.findModerationQueue(ActivityType.ACADEMIC, ReviewStatus.PENDING_MODERATION, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of()));

        ReviewService service = new ReviewService(repository, awarderProvider);
        var response = service.getAcademicModerationQueue(0, 20);

        assertTrue(response.items().isEmpty());
        verify(repository).findModerationQueue(
                ActivityType.ACADEMIC,
                ReviewStatus.PENDING_MODERATION,
                PageRequest.of(0, 20)
        );
    }
}
