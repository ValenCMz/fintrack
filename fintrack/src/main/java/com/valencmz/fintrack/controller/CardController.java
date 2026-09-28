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
import com.valencmz.fintrack.model.dto.card.CardRequest;
import com.valencmz.fintrack.model.dto.card.CardResponse;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.service.card.CardService;

@RestController
@RequestMapping("/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> getAll(
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(cardService.getByUser(userAuth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(cardService.getById(id, userAuth)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CardResponse>> create(
            @Valid @RequestBody CardRequest request,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cardService.create(request, userAuth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CardResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CardRequest request,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(cardService.update(id, request, userAuth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserAuth userAuth) {
        cardService.softDelete(id, userAuth);
        return ResponseEntity.ok(ApiResponse.success("Tarjeta eliminada"));
    }
}
