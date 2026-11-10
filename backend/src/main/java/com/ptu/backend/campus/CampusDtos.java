package com.ptu.backend.campus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

public final class CampusDtos {
    private CampusDtos() {
    }

    public record CreateEventRequest(
            String title,
            String description,
            String date,
            String endsAt,
            String location,
            String category,
            Integer capacity,
            String format,
            Integer groupMinPercent,
            String activityType,
            Integer maxPoints) {
    }

    public record EventView(
            String id,
            String title,
            String description,
            String date,
            String endsAt,
            String location,
            String category,
            int capacity,
            String format,
            Integer groupMinPercent,
            String activityType,
            int maxPoints,
            String creatorId,
            String createdBy,
            BigDecimal rating,
            List<EventGroupRegistrationView> groupRegistrations,
            List<EventParticipantView> participants,
            List<EventReviewView> reviews) {
    }

    public record EventGroupRegistrationView(
            String id, String group, int groupSize, String addedBy, String requestedAt) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EventParticipantView(
            String id,
            String studentId,
            String studentName,
            String group,
            String enrolledAt,
            boolean participationConfirmed,
            String groupRegistrationId,
            BigDecimal awardedPoints,
            Integer awardedPercent,
            String awardedAt) {
    }

    public record EventReviewView(
            String id, String studentId, String studentName, int rating, String comment, String status, String createdAt) {
    }

    public record OrganizerReviewView(
            String id, String eventTitle, String studentName, int rating, String comment, String createdAt) {
    }

    public record EventModerationReviewView(
            String id, String eventId, String eventTitle, String studentName, String group,
            int rating, String comment, String createdAt, List<String> flaggedWords) {
    }

    public record GroupView(String id, String name, int size, String leaderId) {
    }

    public record GroupStudentView(String id, String studentName, String email, String group) {
    }

    public record GroupConfirmationView(
            String id, String studentName, String email, String group, String submittedAt, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroupConfirmationDecision(Boolean approve) {
    }

    public record GroupLeaderRequest(String studentId) {
    }

    public record GroupRegistrationRequest(String groupId) {
    }

    public record AwardRequest(Integer activityPercent) {
    }

    public record ReviewRequest(Integer rating, String comment) {
    }

    public record ReviewDecision(Boolean approved) {
    }

    public record WalletView(BigDecimal balance, List<WalletTransactionView> transactions) {
    }

    public record WalletTransactionView(String id, String title, String detail, BigDecimal amount, String createdAt) {
    }

    public record CampusCaseRewardView(String name, String cosmeticType, String rarity, int chance,
                                       String imageUrl) {
    }

    public record CampusCaseView(String id, String name, String description, BigDecimal price, String imageUrl,
                                 List<CampusCaseRewardView> rewards) {
    }

    public record CatalogView(List<GroupView> groups, BigDecimal caseOpeningPrice,
                              List<CampusCaseRewardView> caseRewards, List<CampusCaseView> cases) {
    }

    public record PassRewardView(String id, String track, int level, String title, String rewardKind, boolean claimed,
                                 String imageUrl) {
    }

    public record PassView(int experience, boolean premiumPassOwned, BigDecimal premiumPrice, String seasonName,
                           String startsAt, String endsAt, List<PassRewardView> rewards) {
    }

    public record AdminPassView(String seasonId, String seasonName, String startsAt, String endsAt,
                                BigDecimal premiumPrice, List<PassSeasonSummary> seasons,
                                List<AdminPassRewardView> rewards, List<AdminCaseOption> cases) {
    }

    public record AdminCaseOption(String id, String name) {
    }

    public record PassSeasonSummary(String id, String seasonName, String startsAt, String endsAt,
                                    BigDecimal premiumPrice, boolean active) {
    }

    public record AdminPassRewardView(String id, String track, int level, String title, String rewardKind,
                                      String cosmeticType, String rarity, String caseId) {
    }

    public record SavePassSeasonRequest(String seasonName, String startsAt, String endsAt, BigDecimal premiumPrice) {
    }

    public record SavePassRewardRequest(String track, Integer level, String title, String rewardKind,
                                        String cosmeticType, String rarity, String caseId) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InventoryItemView(
            String id, String name, String kind, String cosmeticType, String rarity, String source, String obtainedAt,
            String caseId, String imageUrl) {
    }

    public record ProfileCosmeticsView(String avatarItemId, String frameItemId, String backgroundItemId) {
    }

    public record SaveProfileCosmeticsRequest(String avatarItemId, String frameItemId, String backgroundItemId) {
    }

    public record CaseImage(byte[] data, String contentType) {
    }

    public record AdminCaseRewardView(String id, String name, String cosmeticType, String rarity, int weight,
                                      int chance, String imageUrl) {
    }

    public record AdminCaseView(String id, String name, String description, BigDecimal price, boolean active,
                                String imageUrl, List<AdminCaseRewardView> rewards) {
    }

    public record ScheduleRequest(
            String weekday, String startTime, String endTime, String groupId, String disciplineId,
            String teacherId, String room) {
    }

    public record ScheduleView(
            String id, String weekday, String startTime, String endTime, String groupId, String group, String subject,
            String disciplineId, String teacherId, String teacher, String room) {
    }

    public record ScheduleChangeView(
            String id, String type, String message, String groupName, String createdAt) {
    }

    public record ScheduleTimeChangeRequest(String previousStartTime, String previousEndTime,
                                            String startTime, String endTime) {
    }
}
