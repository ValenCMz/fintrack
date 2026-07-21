package com.valencmz.fintrack.errors;

import lombok.Getter;

@Getter
public class Result<T> {
    private final boolean success;
    private final T data;
    private final String errorMessage;

    private Result(T data) {
        this.success = true;
        this.data = data;
        this.errorMessage = null;
    }

    private Result(String errorMessage) {
        this.success = false;
        this.data = null;
        this.errorMessage = errorMessage;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(data);
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>(message);
    }
}
