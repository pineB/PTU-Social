package com.ptu.backend.api;

import com.ptu.backend.api.dto.*;
import com.ptu.backend.domain.ActivityService;
import com.ptu.backend.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FrontendApiController {

    private final ActivityService activityService;
    private final ReviewService reviewService;

    public FrontendApiController(ActivityService activityService, ReviewService reviewService) {
        this.activityService = activityService;
        this.reviewService = reviewService;
    }

    @PostMapping("/activities")
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody ActivityCreateRequest request) {
        return ResponseEntity.ok(activityService.createActivity(request));
    }

    @PostMapping("/activities/{activityId}/join")
    public ResponseEntity<ParticipationResponse> joinActivity(@PathVariable Long activityId,
                                                             @Valid @RequestBody JoinActivityRequest request) {
        return ResponseEntity.ok(activityService.joinActivity(activityId, request));
    }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.ok(reviewService.createReview(request));
    }

    @GetMapping("/moderation/academic/reviews")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReviewModerationQueueResponse> getAcademicReviewQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getAcademicModerationQueue(page, size));
    }

    @PatchMapping("/moderation/academic/reviews/{reviewId}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReviewResponse> moderateReview(@PathVariable Long reviewId,
                                                        @RequestParam boolean approved) {
        return ResponseEntity.ok(reviewService.moderateReview(reviewId, approved));
    }

}
