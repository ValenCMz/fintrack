package com.valencmz.fintrack.model.dto.user;

import com.valencmz.fintrack.enums.Rol;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UsuarioUpdateDTO {

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("rol")
    private Rol rol;

    // Constructor vacío
    public UsuarioUpdateDTO() {
    }

    // Constructor completo
    public UsuarioUpdateDTO(String username, String email, Rol rol) {
        this.username = username;
        this.email = email;
        this.rol = rol;
    }

    // Getters y Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "UsuarioUpdateDTO(username=" + username + ", email=" + email + ", rol=" + rol + ")";
    }
}