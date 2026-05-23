package com.example.mtlsbatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "batch.ssl")
public record SslProperties(
    String clientKeystore,
    String clientKeystorePassword,
    String truststore,          // null = JVMデフォルト(cacerts)を使用
    String truststorePassword
) {}
