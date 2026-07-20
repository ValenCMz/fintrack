package com.valencmz.fintrack.model.dto.account;

import java.util.UUID;

import com.valencmz.fintrack.enums.AccountType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private UUID id;
    private String name;
    private AccountType type;
    private String owner;
    private boolean active;
}
