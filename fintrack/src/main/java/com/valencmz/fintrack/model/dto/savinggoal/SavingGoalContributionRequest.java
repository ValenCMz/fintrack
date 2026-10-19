package com.valencmz.fintrack.model.dto.savinggoal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aporte a una meta de ahorro. Se materializa como un ingreso real desde una
 * cuenta, de modo que el saldo de la cuenta y el progreso de la meta quedan
 * siempre alineados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoalContributionRequest {
    @NotNull
    @Positive
    @Digits(integer = 36, fraction = 2)
    private BigDecimal amount;

    @NotNull
    private LocalDate date;

    @NotNull
    private UUID accountId;

    /** Opcional: si no viene, se usa la categoría global de ingresos. */
    private UUID categoryId;
}
