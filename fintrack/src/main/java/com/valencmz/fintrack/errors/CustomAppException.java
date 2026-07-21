package com.valencmz.fintrack.errors;

import org.springframework.http.HttpStatus;

public class CustomAppException extends RuntimeException {

    private final HttpStatus httpStatus;

    public CustomAppException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
