package com.ptu.backend.review.filter;

import java.util.List;

public class BlockedWordsReviewFilterStrategy implements ReviewFilterStrategy {
    private final List<String> blockedWords;

    public BlockedWordsReviewFilterStrategy(List<String> blockedWords) {
        this.blockedWords = blockedWords == null ? List.of() : blockedWords;
    }

    @Override
    public boolean isValid(String reviewText) {
        if (reviewText == null || reviewText.isBlank()) {
            return false;
        }
        String normalized = reviewText.toLowerCase();
        return blockedWords.stream()
                .map(String::toLowerCase)
                .noneMatch(normalized::contains);
    }
}
