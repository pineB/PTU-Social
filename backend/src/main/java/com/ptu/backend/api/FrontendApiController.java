package com.ptu.backend.api;

import com.ptu.backend.api.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FrontendApiController {

    @PostMapping("/activities")
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody ActivityCreateRequest request) {
        throw new UnsupportedOperationException("Activity creation is not implemented yet");
    }

    @PostMapping("/activities/{activityId}/join")
    public ResponseEntity<ParticipationResponse> joinActivity(@PathVariable Long activityId,
                                                             @Valid @RequestBody JoinActivityRequest request) {
        throw new UnsupportedOperationException("Participation registration is not implemented yet");
    }

    @GetMapping("/teachers/{teacherId}/budget")
    public ResponseEntity<TeacherBudgetResponse> getTeacherBudget(@PathVariable Long teacherId) {
        throw new UnsupportedOperationException("Teacher budget read is not implemented yet");
    }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        throw new UnsupportedOperationException("Review creation is not implemented yet");
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
