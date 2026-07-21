package com.valencmz.fintrack.enums;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Auth
    INVALID_CREDENTIALS("Credenciales inválidas", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS("Ya existe un usuario con ese email", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("No autenticado", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("No tenés permisos", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED("Token expirado", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID("Refresh token inválido", HttpStatus.UNAUTHORIZED),

    // Users
    USER_NOT_FOUND("Usuario no encontrado", HttpStatus.NOT_FOUND),

    // Validation
    VALIDATION_ERROR("Error de validación", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT("Formato de email inválido", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_SHORT("La contraseña debe tener al menos 8 caracteres", HttpStatus.BAD_REQUEST),

    // Generic
    NOT_FOUND("Recurso no encontrado", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR("Error interno del servidor", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String defaultMessage, HttpStatus status) {
        this.defaultMessage = defaultMessage;
        this.status = status;
    }
}