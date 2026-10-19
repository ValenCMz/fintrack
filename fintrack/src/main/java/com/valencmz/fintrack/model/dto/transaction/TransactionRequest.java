package com.valencmz.fintrack.model.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.valencmz.fintrack.enums.TransactionType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class TransactionRequest {
    @NotNull
    private TransactionType type;
    @NotBlank
    @Size(max = 255)
    private String description;
    @NotNull
    @Positive
    @Digits(integer = 36, fraction = 2)
    private BigDecimal amount;
    @NotNull
    private LocalDate date;
    @Size(max = 1000)
    private String notes;
    @NotNull
    private UUID categoryId;
    // accountId o cardId: una compra con tarjeta no toca la cuenta hasta pagarse.
    private UUID accountId;
    private UUID cardId;
    // Cuotas en las que se paga una compra con tarjeta (1-36).
    @Min(1)
    @Max(36)
    private Integer installments;

    public int resolvedInstallments() {
        return installments != null ? installments : 1;
    }
}
