package com.example.mtlsapiclient.service;

import com.example.mtlsapiclient.config.ApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PublicApiService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final ApiProperties api;
    private final ObjectMapper objectMapper;

    public PublicApiService(OkHttpClient okHttpClient, ApiProperties api, ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.api = api;
        this.objectMapper = objectMapper;
    }

    public String get(String path) throws Exception {
        Request request = new Request.Builder()
            .url(api.publicBaseUrl() + path)
            .get()
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "";
        }
    }

    public String post(String path, Map<String, Object> body) throws Exception {
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsString(body), JSON);
        Request request = new Request.Builder()
            .url(api.publicBaseUrl() + path)
            .post(requestBody)
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "";
        }
    }
}
