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

    public record CampusCaseRewardView(String name, String cosmeticType, String rarity, int chance) {
    }

    public record CatalogView(List<GroupView> groups, BigDecimal caseOpeningPrice, List<CampusCaseRewardView> caseRewards) {
    }

    public record PassRewardView(String id, String track, int level, String title, String rewardKind, boolean claimed) {
    }

    public record PassView(int experience, boolean premiumPassOwned, BigDecimal premiumPrice, List<PassRewardView> rewards) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InventoryItemView(
            String id, String name, String kind, String cosmeticType, String rarity, String source, String obtainedAt) {
    }

    public record ScheduleRequest(
            String weekday, String startTime, String endTime, String groupId, String subject, String teacherId, String room) {
    }

    public record ScheduleView(
            String id, String weekday, String startTime, String endTime, String groupId, String group, String subject,
            String teacherId, String teacher, String room) {
    }

    public record ScheduleChangeView(
            String id, String type, String message, String groupName, String createdAt) {
    }

    public record ScheduleTimeChangeRequest(String previousStartTime, String previousEndTime,
                                            String startTime, String endTime) {
    }
}
