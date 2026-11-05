package com.ptu.backend.review.filter;

import java.util.List;
import java.util.Locale;

public class BlockedWordsReviewFilterStrategy implements ReviewFilterStrategy {
    private final List<String> blockedWords;

    public BlockedWordsReviewFilterStrategy(List<String> blockedWords) {
        this.blockedWords = blockedWords == null
            ? List.of()
            : blockedWords.stream().filter(word -> word != null && !word.isBlank()).toList();
    }

    @Override
    public boolean isValid(String reviewText) {
        if (reviewText == null || reviewText.isBlank()) {
            return false;
        }
        String normalized = reviewText.toLowerCase(Locale.ROOT);
        return blockedWords.stream()
            .map(word -> word.toLowerCase(Locale.ROOT))
                .noneMatch(normalized::contains);
    }
}
