package com.ptu.backend.api;

import com.ptu.backend.api.dto.*;
import com.ptu.backend.domain.ActivityService;
import com.ptu.backend.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/teachers/{teacherId}/budget")
    public ResponseEntity<TeacherBudgetResponse> getTeacherBudget(@PathVariable Long teacherId) {
        throw new UnsupportedOperationException("Teacher budget read is not implemented yet");
    }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.ok(reviewService.createReview(request));
    }

    @PatchMapping("/reviews/{reviewId}/moderate")
    public ResponseEntity<ReviewResponse> moderateReview(@PathVariable Long reviewId,
                                                        @RequestParam boolean approved) {
        return ResponseEntity.ok(reviewService.moderateReview(reviewId, approved));
    }

    @GetMapping("/students/{studentId}/battle-pass")
    public ResponseEntity<BattlePassResponse> getBattlePass(@PathVariable Long studentId) {
        throw new UnsupportedOperationException("Battle pass read is not implemented yet");
    }

    @PostMapping("/cases/open")
    public ResponseEntity<InventoryItemResponse> openCase(@Valid @RequestBody CaseOpenRequest request) {
        throw new UnsupportedOperationException("Case opening is not implemented yet");
    }
}
