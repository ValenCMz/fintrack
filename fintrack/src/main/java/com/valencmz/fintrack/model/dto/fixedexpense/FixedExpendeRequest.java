package com.valencmz.fintrack.model.dto.fixedexpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FixedExpendeRequest {
    @NotBlank
    private String name;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotNull
    private LocalDate dueDay;
    private String frequency;
    private boolean active;
    @NotNull
    private UUID categoryId;
    @NotNull
    private UUID accountId;
}
