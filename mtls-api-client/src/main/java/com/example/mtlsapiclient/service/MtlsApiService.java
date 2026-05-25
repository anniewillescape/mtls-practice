package com.example.mtlsapiclient.service;

import com.example.mtlsapiclient.config.MtlsApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

@Service
public class MtlsApiService {

    private final CloseableHttpClient httpClient;
    private final MtlsApiProperties api;
    private final ObjectMapper objectMapper;

    public MtlsApiService(
        CloseableHttpClient httpClient,
        MtlsApiProperties api,
        ObjectMapper objectMapper
    ) {
        this.httpClient = httpClient;
        this.api = api;
        this.objectMapper = objectMapper;
    }

    public String getHello() throws Exception {
        String url = api.baseUrl() + "/api/hello";
        return httpClient.execute(new HttpGet(url), null, response ->
            EntityUtils.toString(response.getEntity())
        );
    }

    public String postEcho(Map<String, Object> body) throws Exception {
        String url = api.baseUrl() + "/api/echo";
        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
        return httpClient.execute(post, null, response ->
            EntityUtils.toString(response.getEntity())
        );
    }
}
