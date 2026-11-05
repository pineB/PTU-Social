package com.ptu.backend.review.filter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpamReviewFilterStrategy implements ReviewFilterStrategy {

    @Override
    public boolean isValid(String reviewText) {
        if (reviewText == null || reviewText.isBlank()) {
            return false;
        }
        String normalized = reviewText.replaceAll("\\s+", " ").trim();
        if (normalized.chars().filter(ch -> ch == '!').count() > 3
                || normalized.matches("(?s).*(.)\\1{7,}.*")) {
            return false;
        }
        String[] words = normalized.toLowerCase(Locale.ROOT).split("\\W+");
        if (words.length < 6) {
            return true;
        }
        Map<String, Long> wordCounts = Arrays.stream(words)
                .filter(word -> !word.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return wordCounts.values().stream().noneMatch(count -> count >= 4 && count * 2 >= words.length);
    }
}
