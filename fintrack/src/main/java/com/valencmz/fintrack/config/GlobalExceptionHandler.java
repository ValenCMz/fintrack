package com.valencmz.fintrack.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.valencmz.fintrack.errors.CustomAppException;

import com.valencmz.fintrack.enums.ErrorCode;
import com.valencmz.fintrack.errors.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomAppException.class)
    public ResponseEntity<ErrorResponse> handleCustom(CustomAppException ex) {
        return buildResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(ErrorCode.INVALID_CREDENTIALS);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UsernameNotFoundException ex) {
        return buildResponse(ErrorCode.USER_NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Error de validación");
        return buildResponse(ErrorCode.VALIDATION_ERROR, msg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return buildResponse(ErrorCode.INTERNAL_ERROR, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode code) {
        ErrorResponse body = new ErrorResponse(code, code.getDefaultMessage(), code.getStatus());
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode code, String message) {
        ErrorResponse body = new ErrorResponse(code, message, code.getStatus());
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode code, String message, HttpStatus status) {
        ErrorResponse body = new ErrorResponse(code, message, status);
        return ResponseEntity.status(status).body(body);
    }
}