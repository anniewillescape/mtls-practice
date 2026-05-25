package com.example.mtlsapiclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client.public-api")
public record PublicApiProperties(
    String baseUrl
) {}
