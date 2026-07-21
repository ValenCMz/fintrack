package com.valencmz.fintrack.model.dto.user;

import java.util.UUID;

import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.model.entity.User;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UserDTO {
    private UUID usuarioId;
    private String username;
    private String email;
    private String password;
    private Rol rol;

    public UserDTO(UUID usuarioId, String username, String email, String password, Rol rol) {
        this.usuarioId = usuarioId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.rol = rol;
    }

    public UserDTO(User usuario) {
        this.usuarioId = usuario.getId();
        this.username = usuario.getUsername();
        this.email = usuario.getEmail();
        this.password = null;
        this.rol = usuario.getRol();
    }

    @Override
    public String toString() {
        return "UsuarioDTO [usuarioId=" + usuarioId + ", username=" + username + ", email=" + email + ", password="
                + password + ", rol=" + rol + "]";
    }
}
