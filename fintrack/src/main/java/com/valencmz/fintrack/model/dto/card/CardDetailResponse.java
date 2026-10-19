package com.valencmz.fintrack.model.dto.card;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.valencmz.fintrack.model.dto.transaction.TransactionResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDetailResponse {
    private UUID id;
    private String holderName;
    private int dueDayOfMonth;
    private LocalDate nextDueDate;
    private boolean active;
    private BigDecimal outstanding;
    private BigDecimal nextInstallmentAmount;
    private LocalDate nextInstallmentDueDate;
    private List<TransactionResponse> purchases;
    private List<CardInstallmentResponse> installments;
    private List<CardPaymentResponse> payments;
}
