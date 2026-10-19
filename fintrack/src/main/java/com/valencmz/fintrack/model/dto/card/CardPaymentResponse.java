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
public class CardPaymentResponse {
    private UUID id;
    private UUID accountId;
    private String accountName;
    private BigDecimal amount;
    private LocalDate date;

    public CardPaymentResponse(com.valencmz.fintrack.model.entity.CardPayment payment) {
        this.id = payment.getId();
        this.accountId = payment.getAccount() != null ? payment.getAccount().getId() : null;
        this.accountName = payment.getAccount() != null ? payment.getAccount().getName() : null;
        this.amount = payment.getAmount();
        this.date = payment.getDate();
    }
}
