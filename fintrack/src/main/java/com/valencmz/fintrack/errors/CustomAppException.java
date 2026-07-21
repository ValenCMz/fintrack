package com.valencmz.fintrack.errors;

import org.springframework.http.HttpStatus;

import com.valencmz.fintrack.enums.ErrorCode;

public class CustomAppException extends RuntimeException {

    private final HttpStatus httpStatus;

    private final ErrorCode errorCode;

    public CustomAppException(String message, HttpStatus httpStatus, ErrorCode errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
