package com.valencmz.fintrack.errors;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class ErrorResponse extends ApiResponse<Object> {
    private ErrorDetail error;

    public ErrorResponse(String message, HttpStatus status) {
        super(false, null, null);
        this.error = new ErrorDetail(status.value(), status.getReasonPhrase(), message);
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorDetail {
        private int status;
        private String statusText;
        private String message;
    }
}
