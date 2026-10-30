package com.ptu.backend.review;

import com.ptu.backend.api.dto.ReviewCreateRequest;
import com.ptu.backend.api.dto.ReviewResponse;
import com.ptu.backend.domain.Review;
import com.ptu.backend.domain.ReviewStatus;
import com.ptu.backend.review.filter.BlockedWordsReviewFilterStrategy;
import com.ptu.backend.review.filter.LengthReviewFilterStrategy;
import com.ptu.backend.review.filter.SpamReviewFilterStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReviewService {

    private final Map<Long, Review> reviews = new ConcurrentHashMap<>();
    private final ReviewModerationService moderationService;
    private final AtomicLong sequence = new AtomicLong(1L);

    public ReviewService() {
        this.moderationService = new ReviewModerationService(List.of(
                new LengthReviewFilterStrategy(20, 1000),
                new BlockedWordsReviewFilterStrategy(List.of("viagra", "buy now", "promo", "click here")),
                new SpamReviewFilterStrategy()
        ));
    }

    public ReviewResponse createReview(ReviewCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Review request is required");
        }
        if (!moderationService.isApproved(request.text())) {
            throw new IllegalArgumentException("Review rejected by moderation filters");
        }

        Review review = new Review();
        review.setId(sequence.getAndIncrement());
        review.setActivityId(request.activityId());
        review.setAuthorId(request.authorId());
        review.setText(request.text());
        review.setRating(request.rating());
        review.setStatus(ReviewStatus.PENDING_MODERATION);
        review.setRewardAwarded(false);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        reviews.put(review.getId(), review);

        return toResponse(review);
    }

    public ReviewResponse moderateReview(Long reviewId, boolean approved) {
        Review review = reviews.get(reviewId);
        if (review == null) {
            throw new IllegalArgumentException("Review not found");
        }

        review.setStatus(approved ? ReviewStatus.APPROVED : ReviewStatus.REJECTED);
        review.setUpdatedAt(LocalDateTime.now());

        return toResponse(review);
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getActivityId(),
                review.getAuthorId(),
                review.getRating(),
                review.getText(),
                review.getStatus(),
                review.isRewardAwarded(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
