package com.valencmz.fintrack.model.dto.account;

import com.valencmz.fintrack.enums.AccountType;
import com.valencmz.fintrack.model.entity.Account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {
    @NotBlank
    @Size(max = 120)
    private String name;
    @NotNull
    private AccountType type;
    @Size(max = 120)
    private String owner;
    private boolean active;

    public Account toEntity() {
        Account account = new Account();
        account.setName(this.name);
        account.setType(this.type);
        account.setOwner(this.owner);
        account.setActive(this.active);
        return account;
    }
}
