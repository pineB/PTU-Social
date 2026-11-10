package com.ptu.backend.campus;

import com.ptu.backend.campus.CampusDtos.*;
import com.ptu.backend.common.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CampusController {
    private final CampusService campus;

    public CampusController(CampusService campus) {
        this.campus = campus;
    }

    @GetMapping("/campus/profile/cosmetics")
    public ProfileCosmeticsView profileCosmetics(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.profileCosmetics(principal);
    }

    @PutMapping("/campus/profile/cosmetics")
    public ProfileCosmeticsView saveProfileCosmetics(@AuthenticationPrincipal UserPrincipal principal,
                                                      @RequestBody SaveProfileCosmeticsRequest request) {
        return campus.saveProfileCosmetics(principal, request);
    }

    @GetMapping("/campus/cases/{id}/image")
    public ResponseEntity<byte[]> caseImage(@PathVariable long id) {
        CaseImage image = campus.caseImage(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType())).body(image.data());
    }

    @GetMapping("/campus/cases/{caseId}/rewards/{rewardId}/image")
    public ResponseEntity<byte[]> caseRewardImage(@PathVariable long caseId, @PathVariable long rewardId) {
        CaseImage image = campus.caseRewardImage(caseId, rewardId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType())).body(image.data());
    }

    @GetMapping("/campus/inventory/{id}/image")
    public ResponseEntity<byte[]> inventoryImage(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable long id) {
        CaseImage image = campus.inventoryImage(principal, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType())).body(image.data());
    }

    @GetMapping("/campus/events")
    public List<EventView> events(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.events(principal);
    }

    @GetMapping("/campus/organizers/{organizerId}/reviews")
    public List<OrganizerReviewView> organizerReviews(@PathVariable UUID organizerId) {
        return campus.organizerReviews(organizerId);
    }

    @PostMapping("/campus/events")
    public EventView createEvent(@AuthenticationPrincipal UserPrincipal principal,
                                 @RequestBody CreateEventRequest request) {
        return campus.createEvent(principal, request);
    }

    @PostMapping("/campus/events/{eventId}/join")
    public void joinEvent(@AuthenticationPrincipal UserPrincipal principal, @PathVariable long eventId) {
        campus.joinEvent(principal, eventId);
    }

    @PostMapping("/campus/events/{eventId}/groups")
    public void registerGroup(@AuthenticationPrincipal UserPrincipal principal, @PathVariable long eventId,
                              @RequestBody GroupRegistrationRequest request) {
        campus.registerGroup(principal, eventId, request);
    }

    @PostMapping("/campus/events/{eventId}/groups/{registrationId}/confirm")
    public void confirmGroupParticipation(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable long eventId, @PathVariable UUID registrationId) {
        campus.confirmGroupParticipation(principal, eventId, registrationId);
    }

    @PostMapping("/campus/events/{eventId}/participants/{participantId}/award")
    public void awardParticipant(@AuthenticationPrincipal UserPrincipal principal, @PathVariable long eventId,
                                 @PathVariable long participantId, @RequestBody AwardRequest request) {
        campus.awardParticipant(principal, eventId, participantId, request);
    }

    @PostMapping("/campus/events/{eventId}/reviews")
    public void createEventReview(@AuthenticationPrincipal UserPrincipal principal, @PathVariable long eventId,
                                  @RequestBody ReviewRequest request) {
        campus.createEventReview(principal, eventId, request);
    }

    @PatchMapping("/campus/events/{eventId}/reviews/{reviewId}")
    public void moderateEventReview(@AuthenticationPrincipal UserPrincipal principal, @PathVariable long eventId,
                                    @PathVariable long reviewId, @RequestBody ReviewDecision request) {
        campus.moderateEventReview(principal, eventId, reviewId, request);
    }

    @GetMapping("/campus/groups")
    public List<GroupView> groups(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.groups(principal);
    }

    @GetMapping("/campus/students")
    public List<GroupStudentView> students(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam UUID groupId) {
        return campus.students(principal, groupId);
    }

    @GetMapping("/campus/group-confirmations")
    public List<GroupConfirmationView> confirmations(@AuthenticationPrincipal UserPrincipal principal,
                                                     @RequestParam UUID groupId) {
        return campus.confirmations(principal, groupId);
    }

    @PostMapping("/campus/group-confirmations/me")
    public GroupConfirmationView requestGroupConfirmation(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.requestGroupConfirmation(principal);
    }

    @GetMapping("/campus/group-confirmations/me")
    public ResponseEntity<GroupConfirmationView> myGroupConfirmation(
            @AuthenticationPrincipal UserPrincipal principal) {
        GroupConfirmationView confirmation = campus.myGroupConfirmation(principal);
        return confirmation == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(confirmation);
    }

    @PatchMapping("/campus/group-confirmations/{id}")
    public void decideGroupConfirmation(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                                        @RequestBody GroupConfirmationDecision request) {
        campus.decideGroupConfirmation(principal, id, request);
    }

    @PutMapping("/campus/groups/{groupId}/leader")
    public void assignGroupLeader(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID groupId,
                                  @RequestBody GroupLeaderRequest request) {
        campus.assignGroupLeader(principal, groupId, request.studentId());
    }

    @GetMapping("/campus/catalog")
    public CatalogView catalog(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.catalog(principal);
    }

    @GetMapping("/wallet/me")
    public WalletView wallet(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.wallet(principal);
    }

    @GetMapping("/campus/schedule")
    public List<ScheduleView> schedule(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.schedule(principal);
    }

    @PostMapping("/campus/schedule")
    public ScheduleView createSchedule(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestBody ScheduleRequest request) {
        return campus.createSchedule(principal, request);
    }

    @PutMapping("/campus/schedule/{scheduleId}")
    public ScheduleView updateSchedule(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable UUID scheduleId, @RequestBody ScheduleRequest request) {
        return campus.updateSchedule(principal, scheduleId, request);
    }

    @DeleteMapping("/campus/schedule/{scheduleId}")
    public void deleteSchedule(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID scheduleId) {
        campus.deleteSchedule(principal, scheduleId);
    }

    @PatchMapping("/campus/schedule/time-slot")
    public void updateScheduleTimeSlot(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestBody ScheduleTimeChangeRequest request) {
        campus.updateScheduleTimeSlot(principal, request);
    }

    @GetMapping("/campus/schedule/changes")
    public List<ScheduleChangeView> scheduleChanges(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.scheduleChanges(principal);
    }

    @GetMapping("/campus/pass")
    public PassView pass(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.pass(principal);
    }

    @PostMapping("/campus/pass/premium")
    public void purchasePremiumPass(@AuthenticationPrincipal UserPrincipal principal) {
        campus.purchasePremiumPass(principal);
    }

    @PostMapping("/campus/pass/rewards/{id}/claim")
    public void claimPassReward(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        campus.claimPassReward(principal, id);
    }

    @GetMapping("/campus/inventory")
    public List<InventoryItemView> inventory(@AuthenticationPrincipal UserPrincipal principal) {
        return campus.inventory(principal);
    }

    @PostMapping("/campus/cases/{id}/open")
    public InventoryItemView openCase(@AuthenticationPrincipal UserPrincipal principal, @PathVariable long id) {
        return campus.openCase(principal, id);
    }
}
