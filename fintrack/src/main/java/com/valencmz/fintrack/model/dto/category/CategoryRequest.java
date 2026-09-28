package com.valencmz.fintrack.model.dto.category;

import com.valencmz.fintrack.enums.CategoryType;
import com.valencmz.fintrack.model.entity.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String color;
    @NotNull
    private CategoryType type;
    private boolean active;

    public Category toEntity() {
        Category category = new Category();
        category.setName(this.name);
        category.setColor(this.color);
        category.setType(this.type);
        category.setActive(this.active);
        return category;
    }
}
