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
public class FixedExpendeResponse {
    private UUID id;
    private String name;
    private BigDecimal amount;
    private LocalDate dueDay;
    private String frequency;
    private boolean active;
    private String categoryName;
    private UUID categoryId;
    private String accountName;
    private UUID accountId;

    public FixedExpendeResponse(com.valencmz.fintrack.model.entity.FixedExpende fixedExpende) {
        this.id = fixedExpende.getId();
        this.name = fixedExpende.getName();
        this.amount = fixedExpende.getAmount();
        this.dueDay = fixedExpende.getDueDay();
        this.frequency = fixedExpende.getFrequency();
        this.active = fixedExpende.isActive();
        this.categoryName = fixedExpende.getCategory() != null ? fixedExpende.getCategory().getName() : null;
        this.categoryId = fixedExpende.getCategory() != null ? fixedExpende.getCategory().getId() : null;
        this.accountName = fixedExpende.getAccount() != null ? fixedExpende.getAccount().getName() : null;
        this.accountId = fixedExpende.getAccount() != null ? fixedExpende.getAccount().getId() : null;
    }
}
