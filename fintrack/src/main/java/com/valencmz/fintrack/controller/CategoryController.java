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
import com.valencmz.fintrack.model.dto.category.CategoryRequest;
import com.valencmz.fintrack.model.dto.category.CategoryResponse;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.service.category.CategoryService;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll(
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getByUser(userAuth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getById(id, userAuth)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryService.create(request, userAuth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.update(id, request, userAuth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserAuth userAuth) {
        categoryService.softDelete(id, userAuth);
        return ResponseEntity.ok(ApiResponse.success("Categoría eliminada"));
    }
}
