package com.valencmz.fintrack.model.dto.category;

import com.valencmz.fintrack.enums.CategoryType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    private String name;
    private String color;
    private CategoryType type;
    private boolean active;
}
