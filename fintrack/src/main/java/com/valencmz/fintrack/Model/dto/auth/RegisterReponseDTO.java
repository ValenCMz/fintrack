package com.valencmz.fintrack.model.dto.auth;

import com.valencmz.fintrack.enums.Rol;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterReponseDTO {

    private String username;
    private String email;
    private Rol rol;

    public RegisterReponseDTO(String username, String email, Rol rol) {
        this.username = username;
        this.email = email;
        this.rol = rol;
    }
}
