package com.valencmz.fintrack.model.dto.fixedexpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
public class FixedExpendeRequest {
    @NotBlank
    @Size(max = 120)
    private String name;
    @NotNull
    @Positive
    @Digits(integer = 36, fraction = 2)
    private BigDecimal amount;
    @NotNull
    private LocalDate dueDay;
    @Size(max = 50)
    private String frequency;
    private boolean active;
    @NotNull
    private UUID categoryId;
    @NotNull
    private UUID accountId;
}
