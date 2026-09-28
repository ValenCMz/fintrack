package com.valencmz.fintrack.model.dto.report;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryExpenseResponse {
    private String categoryName;
    private BigDecimal total;
}
