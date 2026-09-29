package com.valencmz.fintrack.model.dto.monotributo;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.valencmz.fintrack.enums.MonotributoStatus;
import com.valencmz.fintrack.model.entity.Monotributo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonotributoRequest {
    @NotBlank
    private String name;
    @NotNull
    @Positive
    private BigDecimal monthlyAmount;
    @NotNull
    private LocalDate dueDay;
    @NotNull
    private MonotributoStatus status;

    public Monotributo toEntity() {
        Monotributo monotributo = new Monotributo();
        monotributo.setName(this.name);
        monotributo.setMonthlyAmount(this.monthlyAmount);
        monotributo.setDueDay(this.dueDay);
        monotributo.setStatus(this.status);
        return monotributo;
    }
}
