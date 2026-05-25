package com.example.mtlsapiclient.controller;

import com.example.mtlsapiclient.service.MtlsApiService;
import com.example.mtlsapiclient.service.PublicApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/client")
public class ClientController {

    private final MtlsApiService mtlsApiService;
    private final PublicApiService publicApiService;
    private final ObjectMapper objectMapper;

    public ClientController(MtlsApiService mtlsApiService, PublicApiService publicApiService, ObjectMapper objectMapper) {
        this.mtlsApiService = mtlsApiService;
        this.publicApiService = publicApiService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/mtls/hello")
    public Object mtlsHello() throws Exception {
        String json = mtlsApiService.getHello();
        return objectMapper.readValue(json, Object.class);
    }

    @PostMapping("/mtls/echo")
    public Object mtlsEcho(@RequestBody Map<String, Object> body) throws Exception {
        String json = mtlsApiService.postEcho(body);
        return objectMapper.readValue(json, Object.class);
    }

    @GetMapping("/public/get")
    public Object publicGet(@RequestParam(defaultValue = "/get") String path) throws Exception {
        String json = publicApiService.get(path);
        return objectMapper.readValue(json, Object.class);
    }

    @PostMapping("/public/post")
    public Object publicPost(
        @RequestParam(defaultValue = "/post") String path,
        @RequestBody Map<String, Object> body
    ) throws Exception {
        String json = publicApiService.post(path, body);
        return objectMapper.readValue(json, Object.class);
    }
}
