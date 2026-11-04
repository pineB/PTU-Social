package com.ptu.backend.review;

import com.ptu.backend.api.dto.ReviewCreateRequest;
import com.ptu.backend.api.dto.ReviewResponse;
import com.ptu.backend.domain.Review;
import com.ptu.backend.domain.ReviewStatus;
import com.ptu.backend.domain.ActivityType;
import com.ptu.backend.api.dto.ReviewModerationQueueResponse;
import com.ptu.backend.review.filter.BlockedWordsReviewFilterStrategy;
import com.ptu.backend.review.filter.LengthReviewFilterStrategy;
import com.ptu.backend.review.filter.SpamReviewFilterStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ObjectProvider<ReviewCurrencyAwarder> currencyAwarderProvider;
    private final ReviewModerationService moderationService;

    public ReviewService(ReviewRepository reviewRepository,
                         ObjectProvider<ReviewCurrencyAwarder> currencyAwarderProvider) {
        this.reviewRepository = reviewRepository;
        this.currencyAwarderProvider = currencyAwarderProvider;
        this.moderationService = new ReviewModerationService(List.of(
                new LengthReviewFilterStrategy(20, 1000),
                new BlockedWordsReviewFilterStrategy(List.of("viagra", "buy now", "promo", "click here")),
                new SpamReviewFilterStrategy()
        ));
    }

    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Review request is required");
        }
        if (!moderationService.isApproved(request.text())) {
            throw new IllegalArgumentException("Review rejected by moderation filters");
        }

        Review review = new Review();
        review.setActivityId(request.activityId());
        review.setAuthorId(request.authorId());
        review.setText(request.text());
        review.setRating(request.rating());
        review.setStatus(ReviewStatus.PENDING_MODERATION);
        review.setRewardAwarded(false);
        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse moderateReview(Long reviewId, boolean approved) {
        Review review = reviewRepository.findAcademicByIdForUpdate(reviewId, ActivityType.ACADEMIC)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic review not found"));

        ReviewStatus targetStatus = approved ? ReviewStatus.APPROVED : ReviewStatus.REJECTED;
        if (review.getStatus() != ReviewStatus.PENDING_MODERATION && review.getStatus() != targetStatus) {
            throw new IllegalStateException("Review has already been moderated");
        }

        review.setStatus(targetStatus);
        if (approved) {
            awardReviewCurrencyOnce(review);
        }

        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public ReviewModerationQueueResponse getAcademicModerationQueue(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        Page<Review> result = reviewRepository.findModerationQueue(
                ActivityType.ACADEMIC,
                ReviewStatus.PENDING_MODERATION,
                PageRequest.of(page, size)
        );
        return new ReviewModerationQueueResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void awardReviewCurrencyOnce(Review review) {
        if (review.isRewardAwarded()) {
            return;
        }
        ReviewCurrencyAwarder awarder = currencyAwarderProvider.getIfAvailable();
        if (awarder == null) {
            return;
        }
        awarder.awardOnce(review.getId(), review.getAuthorId());
        review.setRewardAwarded(true);
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
