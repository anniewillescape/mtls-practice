package com.example.mtlsbatch.batch;

import com.example.mtlsbatch.config.ApiProperties;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ApiBatchRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiBatchRunner.class);

    private final CloseableHttpClient httpClient;
    private final ApiProperties api;

    public ApiBatchRunner(CloseableHttpClient httpClient, ApiProperties api) {
        this.httpClient = httpClient;
        this.api = api;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Batch job started.");

        String url = api.baseUrl() + "/api/hello";
        log.info("Calling API: {}", url);

        String responseBody = httpClient.execute(new HttpGet(url), response -> {
            log.info("Response status: {}", response.getCode());
            return EntityUtils.toString(response.getEntity());
        });

        log.info("Response body: {}", responseBody);
        log.info("Batch job completed.");
    }
}
