package com.valencmz.fintrack.service.account;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.account.AccountRequest;
import com.valencmz.fintrack.model.dto.account.AccountResponse;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public List<AccountResponse> getByUser(UserAuth userAuth) {
        return accountRepository.findByUserIdAndActive(userAuth.getUser().getId(), true)
                .stream().map(AccountResponse::new).toList();
    }

    public AccountResponse getById(UUID id, UserAuth userAuth) {
        return new AccountResponse(accountRepository.findByIdAndUserId(id, userAuth.getUser().getId())
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND)));
    }

    public AccountResponse create(AccountRequest account, UserAuth userAuth) {
        Account accountEntity = account.toEntity();
        accountEntity.setUser(userAuth.getUser());
        return new AccountResponse(accountRepository.save(accountEntity));
    }

    public AccountResponse update(UUID id, AccountRequest account, UserAuth userAuth) {
        Account existingAccount = accountRepository.findByIdAndUserId(id, userAuth.getUser().getId())
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND));
        existingAccount.setName(account.getName());
        existingAccount.setType(account.getType());
        existingAccount.setOwner(account.getOwner());
        existingAccount.setActive(account.isActive());
        return new AccountResponse(accountRepository.save(existingAccount));
    }

    public void softDelete(UUID id, UserAuth userAuth) {
        Account existingAccount = accountRepository.findByIdAndUserId(id, userAuth.getUser().getId())
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND));
        existingAccount.setActive(false);
        accountRepository.save(existingAccount);
    }

}
