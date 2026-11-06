package com.ptu.backend.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeacherBudgetConcurrencyTest extends com.ptu.backend.support.AbstractIntegrationTest {

    @Autowired
    private TeacherBudgetRepository teacherBudgetRepository;

    @Autowired
    private TeacherBudgetService teacherBudgetService;

    @AfterEach
    void cleanUp() {
        teacherBudgetRepository.deleteAll();
    }

    @Test
    void concurrentAwardsCannotExceedTeacherBudget() throws Exception {
        UUID teacherId = UUID.randomUUID();
        TeacherBudget budget = new TeacherBudget();
        budget.setTeacherId(teacherId);
        budget.setTotalBudget(new BigDecimal("100.00"));
        budget.setSpentBudget(BigDecimal.ZERO);
        teacherBudgetRepository.saveAndFlush(budget);

        int requestCount = 20;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successfulAwards = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            List<Future<?>> requests = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                requests.add(executor.submit(() -> {
                    start.await();
                    try {
                        teacherBudgetService.awardPoints(teacherId, new BigDecimal("20.00"));
                        successfulAwards.incrementAndGet();
                    } catch (ResponseStatusException exceeded) {
                        return null;
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> request : requests) {
                request.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        TeacherBudget updatedBudget = teacherBudgetRepository.findByTeacherId(teacherId).orElseThrow();
        assertEquals(5, successfulAwards.get());
        assertEquals(new BigDecimal("100.00"), updatedBudget.getSpentBudget());
    }
}
