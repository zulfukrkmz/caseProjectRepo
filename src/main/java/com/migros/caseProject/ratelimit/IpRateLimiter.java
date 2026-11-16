package com.migros.caseProject.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class IpRateLimiter {

    private static final long CLEANUP_EVERY_REQUESTS = 256;

    private final RateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();
    private final double bucketIdleExpiryMillis;

    @Autowired
    public IpRateLimiter(RateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    IpRateLimiter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.bucketIdleExpiryMillis = Math.max(1,
                Math.ceil((double) properties.capacity() * properties.refillIntervalMillis()
                        / properties.refillTokens()));
    }

    public RateLimitDecision tryConsume(String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            throw new IllegalArgumentException("Client key must not be blank");
        }

        long now = clock.millis();
        if ((requestCount.incrementAndGet() & (CLEANUP_EVERY_REQUESTS - 1)) == 0) {
            removeExpiredBuckets(now);
        }

        while (true) {
            TokenBucket bucket = buckets.computeIfAbsent(
                    clientKey, ignored -> new TokenBucket(now, properties.capacity()));
            synchronized (bucket) {
                if (buckets.get(clientKey) != bucket) {
                    continue;
                }

                bucket.lastAccessMillis = now;
                refill(bucket, now);
                if (bucket.tokens >= 1) {
                    bucket.tokens -= 1;
                    return RateLimitDecision.permit();
                }

                double millisecondsPerToken =
                        (double) properties.refillIntervalMillis() / properties.refillTokens();
                double retryAfterSecondsExact = (1 - bucket.tokens) * millisecondsPerToken / 1_000;
                long retryAfterSeconds = Math.max(1, (long) Math.ceil(retryAfterSecondsExact));
                return RateLimitDecision.reject(retryAfterSeconds);
            }
        }
    }

    private void refill(TokenBucket bucket, long now) {
        if (now <= bucket.lastRefillMillis) {
            return;
        }

        double elapsedMillis = (double) now - bucket.lastRefillMillis;
        double tokensToAdd = elapsedMillis * properties.refillTokens() / properties.refillIntervalMillis();
        bucket.tokens = Math.min(properties.capacity(), bucket.tokens + tokensToAdd);
        bucket.lastRefillMillis = now;
    }

    private void removeExpiredBuckets(long now) {
        buckets.forEach((clientKey, bucket) -> {
            synchronized (bucket) {
                double idleMillis = (double) now - bucket.lastAccessMillis;
                if (idleMillis >= bucketIdleExpiryMillis) {
                    buckets.remove(clientKey, bucket);
                }
            }
        });
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillMillis;
        private long lastAccessMillis;

        private TokenBucket(long now, long capacity) {
            tokens = capacity;
            lastRefillMillis = now;
            lastAccessMillis = now;
        }
    }
}
