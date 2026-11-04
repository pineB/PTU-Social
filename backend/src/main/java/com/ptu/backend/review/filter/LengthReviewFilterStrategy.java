package com.ptu.backend.review.filter;

public class LengthReviewFilterStrategy implements ReviewFilterStrategy {

    private final int minLength;
    private final int maxLength;

    public LengthReviewFilterStrategy(int minLength, int maxLength) {
        if (minLength < 0 || maxLength < minLength) {
            throw new IllegalArgumentException("Invalid length range");
        }
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public boolean isValid(String reviewText) {
        if (reviewText == null) {
            return false;
        }
        int length = reviewText.trim().length();
        return length >= minLength && length <= maxLength;
    }
}
