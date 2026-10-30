package com.ptu.backend.review.filter;

public interface ReviewFilterStrategy {
    boolean isValid(String reviewText);
}
