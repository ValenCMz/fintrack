package com.valencmz.fintrack.model.dto.debt;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.valencmz.fintrack.enums.DebtStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtRequest {
    private String creditor;
    private BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private LocalDate startDate;
    private DebtStatus status;
}
