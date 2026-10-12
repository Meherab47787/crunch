package com.crustcrunch.exception;

import org.springframework.http.HttpStatus;

/** Base type for errors that map directly to an HTTP status with a user-safe message. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
