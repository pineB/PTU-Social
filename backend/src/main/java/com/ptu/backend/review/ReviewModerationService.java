package com.ptu.backend.review;

import com.ptu.backend.review.filter.ReviewFilterStrategy;

import java.util.List;

public class ReviewModerationService {

    private final List<ReviewFilterStrategy> filters;

    public ReviewModerationService(List<ReviewFilterStrategy> filters) {
        this.filters = filters == null ? List.of() : filters;
    }

    public boolean isApproved(String reviewText) {
        return filters.stream().allMatch(filter -> filter.isValid(reviewText));
    }
}
