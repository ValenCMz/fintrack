package com.valencmz.fintrack.model.dto.debt;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.valencmz.fintrack.enums.DebtStatus;
import com.valencmz.fintrack.model.entity.Debt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtRequest {
    @NotBlank
    private String creditor;
    @Positive
    private BigDecimal totalAmount;
    @PositiveOrZero
    private BigDecimal remainingAmount;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private DebtStatus status;

    public Debt toEntity() {
        Debt debt = new Debt();
        debt.setCreditor(this.creditor);
        debt.setTotalAmount(this.totalAmount);
        debt.setRemainingAmount(this.remainingAmount);
        debt.setStartDate(this.startDate);
        debt.setStatus(this.status);
        return debt;
    }
}
