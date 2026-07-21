package com.valencmz.fintrack.errors;

import org.springframework.http.HttpStatus;

import com.valencmz.fintrack.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class ErrorResponse extends ApiResponse<Object> {
    private ErrorDetail error;

    public ErrorResponse(ErrorCode code, String message, HttpStatus status) {
        super(false, null, null);
        this.error = new ErrorDetail(code.name(), message,
                status.value(), status.getReasonPhrase());
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
        private int status;
        private String statusText;
    }
}
