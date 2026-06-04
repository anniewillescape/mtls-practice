package com.example.mtlsapi.service;

import com.example.mtlsapi.config.RateLimitProperties;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final int maxRequests;
    private final long windowMs;
    private int requestCount = 0;
    private long windowStart = System.currentTimeMillis();

    public RateLimiterService(RateLimitProperties props) {
        this.maxRequests = props.maxRequests();
        this.windowMs = (long) props.windowSeconds() * 1000;
    }

    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        if (now - windowStart >= windowMs) {
            windowStart = now;
            requestCount = 0;
        }
        requestCount++;
        return requestCount <= maxRequests;
    }

    public synchronized long secondsUntilNextWindow() {
        long remainingMs = windowMs - (System.currentTimeMillis() - windowStart);
        return remainingMs <= 0 ? 0 : (long) Math.ceil(remainingMs / 1000.0);
    }
}
