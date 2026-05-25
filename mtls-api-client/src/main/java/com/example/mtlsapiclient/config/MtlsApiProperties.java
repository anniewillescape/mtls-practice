package com.example.mtlsapiclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client.mtls-api")
public record MtlsApiProperties(
    String baseUrl
) {}
