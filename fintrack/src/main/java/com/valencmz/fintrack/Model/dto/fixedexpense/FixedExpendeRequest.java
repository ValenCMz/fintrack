package com.valencmz.fintrack.model.dto.fixedexpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FixedExpendeRequest {
    private String name;
    private BigDecimal amount;
    private LocalDate dueDay;
    private String frequency;
    private boolean active;
    private UUID categoryId;
    private UUID accountId;
}
