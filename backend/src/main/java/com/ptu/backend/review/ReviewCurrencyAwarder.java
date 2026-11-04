package com.ptu.backend.review;

import java.util.UUID;

public interface ReviewCurrencyAwarder {
    void awardOnce(Long reviewId, UUID authorId);
}
