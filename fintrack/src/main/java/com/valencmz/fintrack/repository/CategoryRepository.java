package com.valencmz.fintrack.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valencmz.fintrack.enums.CategoryType;
import com.valencmz.fintrack.model.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    public List<Category> findByUserId(UUID userId);

    public List<Category> findAllByUserIdAndType(UUID userId, CategoryType type);

}
