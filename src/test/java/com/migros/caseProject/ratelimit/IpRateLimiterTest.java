package com.migros.caseProject.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpRateLimiterTest {

    @Test
    void allowsConfiguredBurstThenReturnsRetryDelay() {
        IpRateLimiter limiter = limiter(2, 1, 1_000, new MutableClock());

        assertTrue(limiter.tryConsume("192.0.2.1").allowed());
        assertTrue(limiter.tryConsume("192.0.2.1").allowed());
        RateLimitDecision rejected = limiter.tryConsume("192.0.2.1");

        assertFalse(rejected.allowed());
        assertEquals(1, rejected.retryAfterSeconds());
    }

    @Test
    void refillsTokensOverTimeAndKeepsClientBucketsIndependent() {
        MutableClock clock = new MutableClock();
        IpRateLimiter limiter = limiter(2, 1, 1_000, clock);

        assertTrue(limiter.tryConsume("192.0.2.1").allowed());
        assertTrue(limiter.tryConsume("192.0.2.1").allowed());
        assertFalse(limiter.tryConsume("192.0.2.1").allowed());
        assertTrue(limiter.tryConsume("192.0.2.2").allowed());

        clock.advanceMillis(1_000);
        assertTrue(limiter.tryConsume("192.0.2.1").allowed());
    }

    @Test
    void permitsOnlyTheConfiguredCapacityUnderConcurrentRequests() throws Exception {
        int capacity = 60;
        int requests = 100;
        IpRateLimiter limiter = limiter(capacity, 1, 60_000, new MutableClock());
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < requests; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return limiter.tryConsume("192.0.2.1").allowed();
                }));
            }
            start.countDown();

            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    accepted++;
                }
            }
            assertEquals(capacity, accepted);
        } finally {
            executor.shutdownNow();
        }
    }

    private static IpRateLimiter limiter(long capacity, long refillTokens, long refillIntervalMillis,
                                         Clock clock) {
        return new IpRateLimiter(new RateLimitProperties(capacity, refillTokens, refillIntervalMillis), clock);
    }

    private static final class MutableClock extends Clock {
        private volatile long currentMillis;

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(currentMillis);
        }

        private void advanceMillis(long milliseconds) {
            currentMillis += milliseconds;
        }
    }
}
