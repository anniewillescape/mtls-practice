package com.example.mtlsapiclient;

import com.example.mtlsapiclient.config.ApiProperties;
import com.example.mtlsapiclient.config.SslProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SslProperties.class, ApiProperties.class})
public class MtlsApiClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtlsApiClientApplication.class, args);
    }
}
