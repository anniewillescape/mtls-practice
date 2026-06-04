package com.example.mtlsbatch.batch;

import com.example.mtlsbatch.config.ApiProperties;
import com.example.mtlsbatch.service.ApiCallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiBatchRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiBatchRunner.class);

    private final ApiCallService apiCallService;
    private final ApiProperties api;

    public ApiBatchRunner(ApiCallService apiCallService, ApiProperties api) {
        this.apiCallService = apiCallService;
        this.api = api;
    }

    @Override
    public void run(String... args) throws IOException {
        log.info("Batch job started.");

        String getUrl = api.baseUrl() + "/api/hello";
        log.info("[GET] Calling API: {}", getUrl);
        apiCallService.callGet(getUrl);

        String postUrl = api.baseUrl() + "/api/echo";
        log.info("[POST] Calling API: {}", postUrl);
        apiCallService.callPost(postUrl, """
                {"message": "Hello from batch!", "batchId": "batch-001"}
                """);

        log.info("Batch job completed.");
    }
}
