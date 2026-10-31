package com.ptu.backend.api;

import com.ptu.backend.api.dto.*;
import com.ptu.backend.domain.ActivityService;
import com.ptu.backend.domain.BattlePassRewardService;
import com.ptu.backend.domain.BattlePassSeason;
import com.ptu.backend.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FrontendApiController {

    private final ActivityService activityService;
    private final ReviewService reviewService;
    private final BattlePassRewardService battlePassRewardService;

    public FrontendApiController(ActivityService activityService,
                                ReviewService reviewService,
                                BattlePassRewardService battlePassRewardService) {
        this.activityService = activityService;
        this.reviewService = reviewService;
        this.battlePassRewardService = battlePassRewardService;
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
    public ResponseEntity<TeacherBudgetResponse> getTeacherBudget(@PathVariable UUID teacherId) {
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
    public ResponseEntity<BattlePassResponse> getBattlePass(@PathVariable UUID studentId) {
        throw new UnsupportedOperationException("Battle pass read is not implemented yet");
    }

    @PostMapping("/cases/open")
    public ResponseEntity<InventoryItemResponse> openCase(@Valid @RequestBody CaseOpenRequest request) {
        var item = battlePassRewardService.openCase(request.caseId(), request.studentId());
        return ResponseEntity.ok(new InventoryItemResponse(
                item.getId(),
                item.getCaseRef().getId(),
                item.getOwnerStudentId(),
                item.getItemName(),
                item.getRarity(),
                item.getQuantity(),
                item.getAcquiredAt()
        ));
    }

    @PostMapping("/battle-pass/premium")
    public ResponseEntity<String> purchasePremiumTrack(@Valid @RequestBody PremiumTrackPurchaseRequest request) {
        BattlePassSeason season = new BattlePassSeason();
        season.setId(request.battlePassSeasonId());
        season.setStudentId(request.studentId());
        season.setCurrentLevel(10);
        season.setPremiumTrackPurchased(false);
        battlePassRewardService.purchasePremiumTrack(season, request.studentId(), request.spentCurrency());
        return ResponseEntity.ok("Premium track purchased");
    }
}
