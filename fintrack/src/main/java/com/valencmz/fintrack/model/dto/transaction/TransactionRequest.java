package com.valencmz.fintrack.model.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.valencmz.fintrack.enums.TransactionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    @NotNull
    private TransactionType type;
    @NotBlank
    private String description;
    @Positive
    private BigDecimal amount;
    @NotNull
    private LocalDate date;
    private String notes;
    @NotNull
    private UUID categoryId;
    @NotNull
    private UUID accountId;
}
