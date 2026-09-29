package com.valencmz.fintrack.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

@DisplayName("GlobalExceptionHandler traduce las constraints de PostgreSQL")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ResponseEntity<?> handle(String pgMessage) {
        Throwable cause = new RuntimeException(pgMessage);
        Throwable wrapper = new RuntimeException("could not execute statement", cause);
        DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute", wrapper);
        return handler.handleDataIntegrity(ex);
    }

    @Test
    @DisplayName("null value in column da 409 con mensaje de campo obligatorio")
    void nullValueDaCampoObligatorio() {
        var response = handle("ERROR: null value in column \"amount\" of relation \"account\" violates not-null constraint");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(message(response)).isEqualTo("Falta un campo obligatorio");
    }

    @Test
    @DisplayName("duplicate key value da 409 con mensaje de duplicado")
    void duplicateKeyDaDuplicado() {
        var response = handle("ERROR: duplicate key value violates unique constraint \"account_pkey\"");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(message(response)).isEqualTo("Ya existe un registro con ese valor");
    }

    @Test
    @DisplayName("foreign key violada da 409 con mensaje de registro relacionado")
    void foreignKeyDaRegistroRelacionado() {
        var response = handle("ERROR: insert or update on table \"transaction\" violates foreign key constraint \"fk_account\"");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(message(response)).isEqualTo("No se encontró el registro relacionado");
    }

    @Test
    @DisplayName("check violada da 409 con mensaje generico de restriccion")
    void checkDaRestriccion() {
        var response = handle("ERROR: new row for relation \"account\" violates check constraint \"amount_positive\"");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(message(response)).isEqualTo("Un valor no cumple una restricción de la base");
    }

    @Test
    @DisplayName("una constraint desconocida cae al mensaje por defecto, no a 500")
    void constraintDesconocidaCaeAlDefault() {
        var response = handle("ERROR: something entirely unexpected happened");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(message(response)).isEqualTo("Conflicto con los datos existentes");
    }

    @Test
    @DisplayName("causa sin mensaje no rompe el handler")
    void causaSinMensajeNoRompe() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("vacio", (Throwable) null);

        var response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(message(response)).isEqualTo("Conflicto con los datos existentes");
    }

    @Test
    @DisplayName("el fallback genérico sigue siendo 500")
    void elFallbackSigueSiendo500() {
        var response = handler.handleGeneral(new IllegalStateException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(message(response)).isEqualTo("Error interno del servidor");
    }

    @Test
    @DisplayName("credenciales malas dan 401 con mensaje generico")
    void credencialesMalasDan401() {
        var response = handler.handleBadCredentials();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(message(response)).isEqualTo("Credenciales inválidas");
    }

    @Test
    @DisplayName("sin permiso da 403")
    void sinPermisoDa403() {
        var response = handler.handleAccessDenied(
                new org.springframework.security.access.AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("no se filtra el mensaje interno al cliente")
    void noSeFiltraElMensajeInterno() {
        var response = handler.handleGeneral(
                new IllegalStateException("connection string: postgres://user:secret@host/db"));

        assertThat(message(response))
                .doesNotContain("secret")
                .doesNotContain("postgres://");
    }

    @Test
    @DisplayName("un fallo de conexion cae al 500 y no se reporta como conflicto de datos")
    void falloDeConexionNoEsConflicto() {
        var response = handler.handleGeneral(new CannotGetJdbcConnectionException("no route to host"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String message(ResponseEntity<?> response) {
        Object body = response.getBody();
        assertThat(body).isNotNull();
        return readMessage(body);
    }

    private String readMessage(Object body) {
        try {
            var errorField = body.getClass().getMethod("getError");
            Object error = errorField.invoke(body);
            return (String) error.getClass().getMethod("getMessage").invoke(error);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
