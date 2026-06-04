package com.example.mtlsbatch.exception;

public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String url) {
        super("429 Too Many Requests: " + url);
    }
}
