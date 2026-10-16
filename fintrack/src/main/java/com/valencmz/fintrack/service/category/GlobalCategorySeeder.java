package com.valencmz.fintrack.service.category;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.valencmz.fintrack.enums.CategoryType;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.repository.CategoryRepository;

import jakarta.transaction.Transactional;

@Component
public class GlobalCategorySeeder implements ApplicationRunner {

    private record DefaultCategory(String name, String color, CategoryType type) {
    }

    private static final List<DefaultCategory> DEFAULTS = List.of(
            new DefaultCategory("Supermercado", "#EF4444", CategoryType.EXPENSE),
            new DefaultCategory("Restaurantes", "#F97316", CategoryType.EXPENSE),
            new DefaultCategory("Transporte", "#F59E0B", CategoryType.EXPENSE),
            new DefaultCategory("Vivienda", "#8B5CF6", CategoryType.EXPENSE),
            new DefaultCategory("Servicios", "#3B82F6", CategoryType.EXPENSE),
            new DefaultCategory("Salud", "#EC4899", CategoryType.EXPENSE),
            new DefaultCategory("Educación", "#06B6D4", CategoryType.EXPENSE),
            new DefaultCategory("Ocio", "#10B981", CategoryType.EXPENSE),
            new DefaultCategory("Compras", "#A855F7", CategoryType.EXPENSE),
            new DefaultCategory("Otros", "#64748B", CategoryType.EXPENSE),
            new DefaultCategory("Sueldo", "#22C55E", CategoryType.INCOME),
            new DefaultCategory("Freelance", "#14B8A6", CategoryType.INCOME),
            new DefaultCategory("Inversiones", "#0EA5E9", CategoryType.INCOME),
            new DefaultCategory("Otros ingresos", "#84CC16", CategoryType.INCOME));

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.existsByUserIsNull()) {
            return;
        }
        List<Category> categories = DEFAULTS.stream().map(this::toEntity).toList();
        categoryRepository.saveAll(categories);
    }

    private Category toEntity(DefaultCategory defaultCategory) {
        Category category = new Category();
        category.setName(defaultCategory.name());
        category.setColor(defaultCategory.color());
        category.setType(defaultCategory.type());
        category.setActive(true);
        category.setUser(null);
        return category;
    }
}
