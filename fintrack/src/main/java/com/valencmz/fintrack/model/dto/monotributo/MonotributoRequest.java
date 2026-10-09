package com.valencmz.fintrack.model.dto.monotributo;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.valencmz.fintrack.enums.MonotributoStatus;
import com.valencmz.fintrack.model.entity.Monotributo;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonotributoRequest {
    @NotBlank
    @Size(max = 120)
    private String name;
    @NotNull
    @Positive
    @Digits(integer = 36, fraction = 2)
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
