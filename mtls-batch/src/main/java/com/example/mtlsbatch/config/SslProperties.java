package com.example.mtlsbatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "batch.ssl")
public record SslProperties(
    String clientKeystore,
    String clientKeystorePassword,
    // パブリックCA（Let's Encrypt、DigiCert等）の場合、以下2フィールドは不要
    String truststore,
    String truststorePassword
) {}
