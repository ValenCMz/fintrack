package com.valencmz.fintrack.errors;

import com.valencmz.fintrack.enums.ErrorCode;

import lombok.Getter;

@Getter
public class Result<T> {
    private final boolean success;
    private final T data;
    private final ErrorCode errorCode;
    private final String message;

    private Result(T data) {
        this.success = true;
        this.data = data;
        this.errorCode = null;
        this.message = null;
    }

    private Result(ErrorCode errorCode, String message) {
        this.success = false;
        this.data = null;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(data);
    }

    public static <T> Result<T> failure(ErrorCode errorCode) {
        return new Result<>(errorCode, errorCode.getDefaultMessage());
    }

    public static <T> Result<T> failure(ErrorCode errorCode, String message) {
        return new Result<>(errorCode, message);
    }
}