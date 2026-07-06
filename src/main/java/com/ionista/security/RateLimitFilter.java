package com.ionista.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth";

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate-limit.auth.capacity}")
    private int capacity;

    @Value("${rate-limit.auth.refill-tokens}")
    private int refillTokens;

    @Value("${rate-limit.auth.refill-duration-minutes}")
    private int refillDurationMinutes;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(AUTH_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(clientKey(request), key -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again later.\"}");
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.greedy(refillTokens, Duration.ofMinutes(refillDurationMinutes)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
