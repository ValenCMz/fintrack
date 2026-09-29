package com.valencmz.fintrack.service.category;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.category.CategoryRequest;
import com.valencmz.fintrack.model.dto.category.CategoryResponse;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.CategoryRepository;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<CategoryResponse> getByUser(UserAuth userAuth) {
        return categoryRepository.findByUserIdAndActive(userAuth.getUser().getId(), true)
                .stream().map(CategoryResponse::new).toList();
    }

    public CategoryResponse getById(UUID id, UserAuth userAuth) {
        return new CategoryResponse(categoryRepository.findByIdAndUserId(id, userAuth.getUser().getId())
                .orElseThrow(() -> new CustomAppException("Category not found", HttpStatus.NOT_FOUND)));
    }

    public CategoryResponse create(CategoryRequest category, UserAuth userAuth) {
        Category categoryEntity = category.toEntity();
        categoryEntity.setUser(userAuth.getUser());
        return new CategoryResponse(categoryRepository.save(categoryEntity));
    }

    public CategoryResponse update(UUID id, CategoryRequest category, UserAuth userAuth) {
        Category existingCategory = categoryRepository.findByIdAndUserId(id, userAuth.getUser().getId())
                .orElseThrow(() -> new CustomAppException("Category not found", HttpStatus.NOT_FOUND));
        existingCategory.setName(category.getName());
        existingCategory.setColor(category.getColor());
        existingCategory.setType(category.getType());
        existingCategory.setActive(category.isActive());
        return new CategoryResponse(categoryRepository.save(existingCategory));
    }

    public void softDelete(UUID id, UserAuth userAuth) {
        Category existingCategory = categoryRepository.findByIdAndUserId(id, userAuth.getUser().getId())
                .orElseThrow(() -> new CustomAppException("Category not found", HttpStatus.NOT_FOUND));
        existingCategory.setActive(false);
        categoryRepository.save(existingCategory);
    }

}
