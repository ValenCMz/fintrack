package com.valencmz.fintrack.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.valencmz.fintrack.errors.ApiResponse;
import com.valencmz.fintrack.model.dto.user.UserDTO;
import com.valencmz.fintrack.model.dto.user.UserResponse;
import com.valencmz.fintrack.model.dto.user.UsuarioUpdateDTO;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.service.UsuarioService;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(usuarioService.getUserInfo(userAuth))));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        List<UserResponse> users = usuarioService.getAllUsers().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> update(@PathVariable UUID id,
            @RequestBody UsuarioUpdateDTO dto) {
        usuarioService.updateUser(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Usuario actualizado"));
    }

    private UserResponse toResponse(UserDTO dto) {
        return new UserResponse(dto.getUsuarioId(), dto.getUsername(), dto.getEmail());
    }
}
