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
        return matchingWords(reviewText).isEmpty();
    }

    public List<String> matchingWords(String reviewText) {
        if (reviewText == null || reviewText.isBlank()) {
            return List.of();
        }
        String normalized = normalize(reviewText);
        return blockedWords.stream()
            .filter(word -> normalized.contains(normalize(word)))
            .distinct()
            .toList();
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('ё', 'е');
    }
}
