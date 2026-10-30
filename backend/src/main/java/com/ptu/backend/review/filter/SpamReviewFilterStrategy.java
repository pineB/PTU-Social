package com.ptu.backend.review.filter;

public class SpamReviewFilterStrategy implements ReviewFilterStrategy {

    @Override
    public boolean isValid(String reviewText) {
        if (reviewText == null || reviewText.isBlank()) {
            return false;
        }
        String normalized = reviewText.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 0 && normalized.chars().filter(ch -> ch == '!').count() > 3) {
            return false;
        }
        String[] words = normalized.split("\\s+");
        if (words.length >= 5) {
            long duplicates = java.util.Arrays.stream(words)
                    .filter(word -> java.util.regex.Pattern.compile("(?i)\\b" + java.util.regex.Pattern.quote(word) + "\\b").matcher(normalized).results().count() > 1)
                    .distinct()
                    .count();
            if (duplicates > 0) {
                return false;
            }
        }
        return true;
    }
}
