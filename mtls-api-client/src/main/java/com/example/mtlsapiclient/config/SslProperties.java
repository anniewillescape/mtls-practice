package com.example.mtlsapiclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client.ssl")
public record SslProperties(
    String clientKeystore,
    String clientKeystorePassword,
    String truststore,
    String truststorePassword
) {}
