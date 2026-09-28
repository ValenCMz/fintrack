package com.valencmz.fintrack.service.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.enums.TransactionType;
import com.valencmz.fintrack.model.dto.report.AccountBalanceResponse;
import com.valencmz.fintrack.model.dto.report.CategoryExpenseResponse;
import com.valencmz.fintrack.model.dto.report.MonthlySummaryResponse;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.Transaction;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;
import com.valencmz.fintrack.repository.TransactionRepository;

@Service
public class ReportService {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;

    public MonthlySummaryResponse monthlySummary(UserAuth userAuth, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(
                userAuth.getUser().getId(), ym.atDay(1), ym.atEndOfMonth());

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
            }
        }
        return new MonthlySummaryResponse(year, month, income, expense, income.subtract(expense));
    }

    public List<CategoryExpenseResponse> byCategory(UserAuth userAuth, LocalDate from, LocalDate to) {
        List<Transaction> transactions = (from != null && to != null)
                ? transactionRepository.findByUserIdAndDateBetween(userAuth.getUser().getId(), from, to)
                : transactionRepository.findByUserId(userAuth.getUser().getId());

        Map<String, BigDecimal> byCategory = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                String name = t.getCategory() != null ? t.getCategory().getName() : "Sin categoría";
                byCategory.merge(name, t.getAmount(), BigDecimal::add);
            }
        }

        List<CategoryExpenseResponse> result = new ArrayList<>();
        byCategory.forEach((name, total) -> result.add(new CategoryExpenseResponse(name, total)));
        return result;
    }

    public List<AccountBalanceResponse> balance(UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        List<Account> accounts = accountRepository.findByUserId(userId);
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        Map<UUID, BigDecimal> balances = new HashMap<>();
        Map<UUID, String> names = new HashMap<>();
        for (Account a : accounts) {
            balances.put(a.getId(), BigDecimal.ZERO);
            names.put(a.getId(), a.getName());
        }

        for (Transaction t : transactions) {
            UUID accountId = t.getAccount() != null ? t.getAccount().getId() : null;
            if (accountId == null) {
                continue;
            }
            BigDecimal current = balances.getOrDefault(accountId, BigDecimal.ZERO);
            if (t.getType() == TransactionType.INCOME) {
                current = current.add(t.getAmount());
            } else {
                current = current.subtract(t.getAmount());
            }
            balances.put(accountId, current);
        }

        List<AccountBalanceResponse> result = new ArrayList<>();
        balances.forEach((id, b) -> result.add(new AccountBalanceResponse(id, names.get(id), b)));
        return result;
    }
}
