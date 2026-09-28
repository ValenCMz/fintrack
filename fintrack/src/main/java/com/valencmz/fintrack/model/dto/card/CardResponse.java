package com.valencmz.fintrack.model.dto.card;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private UUID id;
    private String holderName;
    private LocalDate dueDay;
    private BigDecimal amount;
    private boolean active;

    public CardResponse(com.valencmz.fintrack.model.entity.Card card) {
        this.id = card.getId();
        this.holderName = card.getHolderName();
        this.dueDay = card.getDueDay();
        this.amount = card.getAmount();
        this.active = card.isActive();
    }
}
