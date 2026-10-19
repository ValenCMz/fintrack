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
    private int dueDayOfMonth;
    private LocalDate nextDueDate;
    private boolean active;
    private BigDecimal outstanding;
    private BigDecimal nextInstallmentAmount;
    private LocalDate nextInstallmentDueDate;

    public CardResponse(com.valencmz.fintrack.model.entity.Card card, LocalDate nextDueDate, BigDecimal outstanding,
            BigDecimal nextInstallmentAmount, LocalDate nextInstallmentDueDate) {
        this.id = card.getId();
        this.holderName = card.getHolderName();
        this.dueDayOfMonth = card.getDueDayOfMonth();
        this.nextDueDate = nextDueDate;
        this.active = card.isActive();
        this.outstanding = outstanding;
        this.nextInstallmentAmount = nextInstallmentAmount;
        this.nextInstallmentDueDate = nextInstallmentDueDate;
    }
}
