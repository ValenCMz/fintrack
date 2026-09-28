package com.valencmz.fintrack.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.valencmz.fintrack.errors.ApiResponse;
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
    public ResponseEntity<ApiResponse<?>> login(@RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.success(this.usuarioService.login(loginDTO, response)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody RegisterDTO registerDTO) {
        return ResponseEntity.ok(ApiResponse.success(this.usuarioService.register(registerDTO)));
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<ApiResponse<?>> refreshAccess(
            @CookieValue("refreshTokenFinTrack") String refreshTokenString,
            HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.success(this.jwtService.refresh(refreshTokenString, response)));
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        this.usuarioService.forgotPassword(forgotPasswordDTO);
        return ResponseEntity.ok(ApiResponse.success("Si esta cuenta existe recibirás un mail"));
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody ResetPasswordRequestDTO resetPasswordRequestDTO) {
        this.usuarioService.resetPassword(resetPasswordRequestDTO);
        return ResponseEntity.ok(ApiResponse.success("Contraseña actualizada correctamente"));
    }
}
