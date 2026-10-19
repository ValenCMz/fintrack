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
public class CardInstallmentResponse {
    private UUID id;
    private int number;
    private int installments;
    private BigDecimal amount;
    private LocalDate dueDate;
    private boolean paid;
    private LocalDate paidDate;
    private UUID transactionId;
    private String description;

    public CardInstallmentResponse(com.valencmz.fintrack.model.entity.CardInstallment installment) {
        this.id = installment.getId();
        this.number = installment.getNumber();
        this.installments = installment.getTransaction().getInstallments();
        this.amount = installment.getAmount();
        this.dueDate = installment.getDueDate();
        this.paid = installment.isPaid();
        this.paidDate = installment.getPaidDate();
        this.transactionId = installment.getTransaction().getId();
        this.description = installment.getTransaction().getDescription();
    }
}
