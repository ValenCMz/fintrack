package com.valencmz.fintrack.model.dto.account;

import com.valencmz.fintrack.enums.AccountType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {
    private String name;
    private AccountType type;
    private String owner;
    private boolean active;
}
