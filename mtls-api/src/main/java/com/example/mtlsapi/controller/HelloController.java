package com.example.mtlsapi.controller;

import com.example.mtlsapi.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    private final RateLimiterService rateLimiter;

    public HelloController(RateLimiterService rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/hello")
    public Map<String, String> hello(HttpServletRequest request) {
        checkRateLimit("GET /api/hello");

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
        checkRateLimit("POST /api/echo");

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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        if (ex.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(rateLimiter.secondsUntilNextWindow()))
                    .body(Map.of("error", "Too Many Requests", "message", ex.getReason()));
        }
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() != null ? ex.getReason() : "Error"));
    }

    private void checkRateLimit(String endpoint) {
        if (!rateLimiter.tryAcquire()) {
            log.warn("Rate limit exceeded for {}", endpoint);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please try again later.");
        }
    }
}
