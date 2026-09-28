package com.valencmz.fintrack.model.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.valencmz.fintrack.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private TransactionType type;
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private String notes;
    private String categoryName;
    private String accountName;

    public TransactionResponse(com.valencmz.fintrack.model.entity.Transaction transaction) {
        this.id = transaction.getId();
        this.type = transaction.getType();
        this.description = transaction.getDescription();
        this.amount = transaction.getAmount();
        this.date = transaction.getDate();
        this.notes = transaction.getNotes();
        this.categoryName = transaction.getCategory() != null ? transaction.getCategory().getName() : null;
        this.accountName = transaction.getAccount() != null ? transaction.getAccount().getName() : null;
    }
}
