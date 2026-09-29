package com.valencmz.fintrack.model.dto.report;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectionResponse {
    private String month;
    private BigDecimal estimatedIncome;
    private BigDecimal estimatedExpense;
    private BigDecimal estimatedNet;
}
