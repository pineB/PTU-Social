package com.ptu.backend.review.filter;

public class MinLengthReviewFilterStrategy implements ReviewFilterStrategy {
    private final int minimumLength;

    public MinLengthReviewFilterStrategy(int minimumLength) {
        if (minimumLength < 0) {
            throw new IllegalArgumentException("Minimum length must be non-negative");
        }
        this.minimumLength = minimumLength;
    }

    @Override
    public boolean isValid(String reviewText) {
        return reviewText != null && reviewText.trim().length() >= minimumLength;
    }
}
