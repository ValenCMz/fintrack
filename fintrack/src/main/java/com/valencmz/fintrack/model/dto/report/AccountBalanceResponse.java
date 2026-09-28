package com.valencmz.fintrack.model.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {
    private UUID accountId;
    private String accountName;
    private BigDecimal balance;
}
