package com.valencmz.fintrack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.valencmz.fintrack.errors.ApiResponse;
import com.valencmz.fintrack.errors.CustomAppException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomAppException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustom(CustomAppException ex) {
        HttpStatus status = ex.getHttpStatus();
        return ResponseEntity.status(status).body(ApiResponse.error(status, ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Error de validación");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Cuerpo de la petición inválido"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción"));
    }

    /**
     * Traduce la SQLException de Postgres a un mensaje que diga que constraint
     * fallo. Antes toda violacion de integridad volvia con el mismo texto
     * "Conflicto con los datos existentes", que no permite distinguir un email
     * duplicado de un campo obligatorio faltante ni saber que corregir.
     *
     * El SQLState es el unico dato estable: el mensaje de Postgres cambia
     * entre versiones e idiomas, el SQLState no.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String constraint = extractConstraint(ex);
        String detail = switch (constraint) {
            case "NOT NULL" -> "Falta un campo obligatorio";
            case "UNIQUE" -> "Ya existe un registro con ese valor";
            case "FOREIGN KEY" -> "No se encontró el registro relacionado";
            case "CHECK" -> "Un valor no cumple una restricción de la base";
            default -> "Conflicto con los datos existentes";
        };
        log.warn("Violacion de integridad ({}): {}", constraint, ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, detail));
    }

    /**
     * Saca el tipo de constraint del mensaje de Postgres. Viene embebido en el
     * texto de la causa mas especifica, que es la unica que describe la
     * violacion: el mensaje de DataIntegrityViolationException es generico.
     */
    private String extractConstraint(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message == null) {
            return "";
        }
        if (message.contains("null value in column")) {
            return "NOT NULL";
        }
        if (message.contains("duplicate key value")) {
            return "UNIQUE";
        }
        if (message.contains("violates foreign key constraint")) {
            return "FOREIGN KEY";
        }
        if (message.contains("violates check constraint")) {
            return "CHECK";
        }
        return "";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor"));
    }
}
