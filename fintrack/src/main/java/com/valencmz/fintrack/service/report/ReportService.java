package com.valencmz.fintrack.service.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.valencmz.fintrack.model.dto.report.ProjectionResponse;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.Transaction;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;
import com.valencmz.fintrack.repository.TransactionRepository;

@Service
public class ReportService {

    private static final int HISTORY_MONTHS = 3;

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

    /**
     * Proyecta los proximos meses usando el promedio de ingresos y egresos de
     * los ultimos meses cerrados. No incluye gastos fijos: su frecuencia es un
     * String libre y no se puede normalizar a mensual de forma confiable. El
     * promedio historico ya los arrastra porque son transacciones.
     */
    public List<ProjectionResponse> projections(UserAuth userAuth, int months) {
        YearMonth thisMonth = YearMonth.now();
        YearMonth firstMonth = thisMonth.minusMonths(HISTORY_MONTHS);

        List<Transaction> history = transactionRepository.findByUserIdAndDateBetween(
                userAuth.getUser().getId(), firstMonth.atDay(1), thisMonth.atDay(1).minusDays(1));

        if (history.isEmpty()) {
            return List.of();
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Transaction t : history) {
            if (t.getType() == TransactionType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
            }
        }

        BigDecimal avgIncome = income.divide(BigDecimal.valueOf(HISTORY_MONTHS), 2, RoundingMode.HALF_UP);
        BigDecimal avgExpense = expense.divide(BigDecimal.valueOf(HISTORY_MONTHS), 2, RoundingMode.HALF_UP);

        List<ProjectionResponse> result = new ArrayList<>();
        for (int i = 1; i <= months; i++) {
            result.add(new ProjectionResponse(
                    thisMonth.plusMonths(i).toString(),
                    avgIncome,
                    avgExpense,
                    avgIncome.subtract(avgExpense)));
        }
        return result;
    }
}
