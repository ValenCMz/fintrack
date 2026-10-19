package com.valencmz.fintrack.model.dto.card;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pago de una cuota o de un detalle completo. El monto siempre lo calcula el
 * servidor: el cliente solo elige desde qué cuenta y con qué fecha.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardInstallmentPaymentRequest {
    @NotNull
    private UUID accountId;
    @NotNull
    private LocalDate date;
}
