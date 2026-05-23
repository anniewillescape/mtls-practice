package com.example.mtlsapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    public Map<String, String> hello(HttpServletRequest request) {
        X509Certificate[] certs =
                (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");

        String clientDN = (certs != null && certs.length > 0)
                ? certs[0].getSubjectX500Principal().getName()
                : "unknown";

        log.info("Received GET /api/hello from client: {}", clientDN);

        return Map.of(
                "message", "Hello from mTLS API!",
                "clientDN", clientDN,
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        X509Certificate[] certs =
                (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");

        String clientDN = (certs != null && certs.length > 0)
                ? certs[0].getSubjectX500Principal().getName()
                : "unknown";

        log.info("Received POST /api/echo from client: {}", clientDN);

        Map<String, Object> response = new HashMap<>();
        response.put("echo", body);
        response.put("clientDN", clientDN);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}
