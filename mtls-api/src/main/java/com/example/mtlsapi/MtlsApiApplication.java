package com.example.mtlsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MtlsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtlsApiApplication.class, args);
    }
}
