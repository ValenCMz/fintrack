package com.valencmz.fintrack.model.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class LoginDTO {

    @NotBlank(message = "El email es obligatorio")
    @Size(max = 254, message = "El email no puede superar los 254 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
    private String password;

    public LoginDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

}
