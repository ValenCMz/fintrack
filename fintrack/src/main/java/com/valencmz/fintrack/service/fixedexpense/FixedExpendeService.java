package com.valencmz.fintrack.service.fixedexpense;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest;
import com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeResponse;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.model.entity.FixedExpende;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;
import com.valencmz.fintrack.repository.CategoryRepository;
import com.valencmz.fintrack.repository.FixedExpendeRepository;

@Service
public class FixedExpendeService {

    @Autowired
    private FixedExpendeRepository fixedExpendeRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AccountRepository accountRepository;

    public List<FixedExpendeResponse> getByUser(UserAuth userAuth) {
        return fixedExpendeRepository.findByUserId(userAuth.getUser().getId())
                .stream().map(FixedExpendeResponse::new).toList();
    }

    public FixedExpendeResponse getById(UUID id, UserAuth userAuth) {
        return new FixedExpendeResponse(findOwnedFixedExpende(id, userAuth.getUser().getId()));
    }

    public FixedExpendeResponse create(FixedExpendeRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new CustomAppException("Category not found", HttpStatus.NOT_FOUND));
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND));

        FixedExpende fixedExpende = new FixedExpende();
        fixedExpende.setName(request.getName());
        fixedExpende.setAmount(request.getAmount());
        fixedExpende.setDueDay(request.getDueDay());
        fixedExpende.setFrequency(request.getFrequency());
        fixedExpende.setActive(request.isActive());
        fixedExpende.setCategory(category);
        fixedExpende.setAccount(account);
        fixedExpende.setUser(userAuth.getUser());
        return new FixedExpendeResponse(fixedExpendeRepository.save(fixedExpende));
    }

    public FixedExpendeResponse update(UUID id, FixedExpendeRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        FixedExpende fixedExpende = findOwnedFixedExpende(id, userId);
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new CustomAppException("Category not found", HttpStatus.NOT_FOUND));
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND));

        fixedExpende.setName(request.getName());
        fixedExpende.setAmount(request.getAmount());
        fixedExpende.setDueDay(request.getDueDay());
        fixedExpende.setFrequency(request.getFrequency());
        fixedExpende.setActive(request.isActive());
        fixedExpende.setCategory(category);
        fixedExpende.setAccount(account);
        return new FixedExpendeResponse(fixedExpendeRepository.save(fixedExpende));
    }

    public void softDelete(UUID id, UserAuth userAuth) {
        FixedExpende fixedExpende = findOwnedFixedExpende(id, userAuth.getUser().getId());
        fixedExpende.setActive(false);
        fixedExpendeRepository.save(fixedExpende);
    }

    private FixedExpende findOwnedFixedExpende(UUID id, UUID userId) {
        return fixedExpendeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomAppException("Fixed expense not found", HttpStatus.NOT_FOUND));
    }
}
