package com.migros.caseProject.ratelimit;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        @Min(1) long capacity,
        @Min(1) long refillTokens,
        @Min(1) long refillIntervalMillis) {
}
