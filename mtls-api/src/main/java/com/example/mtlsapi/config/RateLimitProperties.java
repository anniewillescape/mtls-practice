package com.example.mtlsapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api.rate-limit")
public record RateLimitProperties(int maxRequests, int windowSeconds) {}
