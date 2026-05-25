package com.example.mtlsapiclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client.api")
public record ApiProperties(
    String mtlsBaseUrl,
    String publicBaseUrl
) {}
