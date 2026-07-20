package com.valencmz.fintrack.model.dto.card;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {
    private String holderName;
    private LocalDate dueDay;
    private BigDecimal amount;
    private boolean active;
}
