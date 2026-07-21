package com.valencmz.fintrack.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.valencmz.fintrack.model.dto.auth.ForgotPasswordDTO;
import com.valencmz.fintrack.model.dto.auth.LoginDTO;
import com.valencmz.fintrack.model.dto.auth.RegisterDTO;
import com.valencmz.fintrack.model.dto.auth.ResetPasswordRequestDTO;
import com.valencmz.fintrack.service.JwtService;
import com.valencmz.fintrack.service.UsuarioService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        return ResponseEntity.ok(this.usuarioService.login(loginDTO, response));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO registerDTO) {
        try {
            return ResponseEntity.ok(this.usuarioService.register(registerDTO));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<?> refreshAccess(
            @CookieValue("refreshTokenFinTrack") String refreshTokenString,
            HttpServletResponse response) {
        return ResponseEntity.ok(this.jwtService.refresh(refreshTokenString, response));
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        this.usuarioService.forgotPassword(forgotPasswordDTO);
        return ResponseEntity.ok("Si esta cuenta existe recibiras un mail");
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO resetPasswordRequestDTO) {
        try {
            this.usuarioService.resetPassword(resetPasswordRequestDTO);
            return ResponseEntity.ok("Contraseña actualizada correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", e.getMessage() != null ? e.getMessage() : "Error al actualizar la contraseña"));
        }
    }

}
