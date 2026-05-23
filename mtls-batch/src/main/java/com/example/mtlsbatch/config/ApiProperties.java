package com.example.mtlsbatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "batch.api")
public record ApiProperties(String baseUrl) {}
