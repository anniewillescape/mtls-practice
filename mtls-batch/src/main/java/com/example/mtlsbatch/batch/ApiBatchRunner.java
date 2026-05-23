package com.example.mtlsbatch.batch;

import com.example.mtlsbatch.config.ApiProperties;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
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

        callGet();
        callPost();

        log.info("Batch job completed.");
    }

    private void callGet() throws Exception {
        String url = api.baseUrl() + "/api/hello";
        log.info("[GET] Calling API: {}", url);

        String responseBody = httpClient.execute(new HttpGet(url), (HttpContext) null, response -> {
            log.info("[GET] Response status: {}", response.getCode());
            return EntityUtils.toString(response.getEntity());
        });

        log.info("[GET] Response body: {}", responseBody);
    }

    private void callPost() throws Exception {
        String url = api.baseUrl() + "/api/echo";
        log.info("[POST] Calling API: {}", url);

        String requestJson = """
                {"message": "Hello from batch!", "batchId": "batch-001"}
                """;

        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));

        String responseBody = httpClient.execute(post, (HttpContext) null, response -> {
            log.info("[POST] Response status: {}", response.getCode());
            return EntityUtils.toString(response.getEntity());
        });

        log.info("[POST] Response body: {}", responseBody);
    }
}
