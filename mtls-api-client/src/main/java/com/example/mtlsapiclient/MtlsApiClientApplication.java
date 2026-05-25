package com.example.mtlsapiclient;

import com.example.mtlsapiclient.config.MtlsApiProperties;
import com.example.mtlsapiclient.config.PublicApiProperties;
import com.example.mtlsapiclient.config.SslProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SslProperties.class, MtlsApiProperties.class, PublicApiProperties.class})
public class MtlsApiClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtlsApiClientApplication.class, args);
    }
}
