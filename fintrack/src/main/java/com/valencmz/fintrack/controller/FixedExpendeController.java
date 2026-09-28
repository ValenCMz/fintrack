package com.valencmz.fintrack.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.valencmz.fintrack.errors.ApiResponse;
import com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest;
import com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeResponse;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.service.fixedexpense.FixedExpendeService;

@RestController
@RequestMapping("/fixed-expenses")
public class FixedExpendeController {

    @Autowired
    private FixedExpendeService fixedExpendeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FixedExpendeResponse>>> getAll(
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(fixedExpendeService.getByUser(userAuth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FixedExpendeResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(fixedExpendeService.getById(id, userAuth)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FixedExpendeResponse>> create(
            @Valid @RequestBody FixedExpendeRequest request,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(fixedExpendeService.create(request, userAuth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FixedExpendeResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody FixedExpendeRequest request,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(fixedExpendeService.update(id, request, userAuth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserAuth userAuth) {
        fixedExpendeService.softDelete(id, userAuth);
        return ResponseEntity.ok(ApiResponse.success("Gasto fijo eliminado"));
    }
}
