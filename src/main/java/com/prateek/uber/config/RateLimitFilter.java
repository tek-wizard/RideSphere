package com.prateek.uber.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedissonClient redissonClient;
    private static final long RATE_LIMIT = 10;
    private static final long RATE_INTERVAL = 60; // seconds

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        String redisKey = "rate_limit:" + clientIp;

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(redisKey);

        // Initialize rule if key doesn't exist: 10 req / 60 sec
        if (!rateLimiter.isExists()) {
            rateLimiter.trySetRate(RateType.OVERALL, RATE_LIMIT, RATE_INTERVAL, RateIntervalUnit.SECONDS);
            rateLimiter.expire(1, TimeUnit.HOURS); // cleanup stale IPs
        }

        if (rateLimiter.tryAcquire()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests.");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Bypass rate limiting for WebSocket handshake and static resources
        return path.startsWith("/ws") || path.startsWith("/css");
    }
}