package com.migros.caseProject.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.caseProject.model.data.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_MESSAGE = "Rate limit exceeded. Please retry later.";

    private final IpRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(IpRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String clientAddress = request.getRemoteAddr();
        if (clientAddress == null || clientAddress.isBlank()) {
            throw new IllegalStateException("Could not determine the request's remote address");
        }

        RateLimitDecision decision = rateLimiter.tryConsume(clientAddress);
        if (decision.allowed()) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiError(RATE_LIMIT_MESSAGE));
        return false;
    }
}
