package com.valencmz.fintrack.service.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.enums.TransactionType;
import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.transaction.TransactionRequest;
import com.valencmz.fintrack.model.dto.transaction.TransactionResponse;
import com.valencmz.fintrack.model.dto.transaction.TransactionSummaryResponse;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.model.entity.Transaction;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;
import com.valencmz.fintrack.repository.CategoryRepository;
import com.valencmz.fintrack.repository.TransactionRepository;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AccountRepository accountRepository;

    public List<TransactionResponse> getByUser(UserAuth userAuth) {
        return transactionRepository.findByUserIdOrderByDateDesc(userAuth.getUser().getId())
                .stream().map(TransactionResponse::new).toList();
    }

    public TransactionResponse getById(UUID id, UserAuth userAuth) {
        return new TransactionResponse(findOwnedTransaction(id, userAuth.getUser().getId()));
    }

    public TransactionResponse create(TransactionRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new CustomAppException("Category not found", HttpStatus.NOT_FOUND));
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND));

        Transaction transaction = new Transaction();
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setUser(userAuth.getUser());
        return new TransactionResponse(transactionRepository.save(transaction));
    }

    public TransactionResponse update(UUID id, TransactionRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        Transaction transaction = findOwnedTransaction(id, userId);
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new CustomAppException("Category not found", HttpStatus.NOT_FOUND));
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND));

        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());
        transaction.setCategory(category);
        transaction.setAccount(account);
        return new TransactionResponse(transactionRepository.save(transaction));
    }

    public void delete(UUID id, UserAuth userAuth) {
        transactionRepository.delete(findOwnedTransaction(id, userAuth.getUser().getId()));
    }

    public TransactionSummaryResponse getSummary(UserAuth userAuth, LocalDate from, LocalDate to) {
        UUID userId = userAuth.getUser().getId();
        List<Transaction> transactions = (from != null && to != null)
                ? transactionRepository.findByUserIdAndDateBetween(userId, from, to)
                : transactionRepository.findByUserId(userId);

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
            }
        }
        return new TransactionSummaryResponse(income, expense, income.subtract(expense), from, to);
    }

    private Transaction findOwnedTransaction(UUID id, UUID userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomAppException("Transaction not found", HttpStatus.NOT_FOUND));
    }
}
