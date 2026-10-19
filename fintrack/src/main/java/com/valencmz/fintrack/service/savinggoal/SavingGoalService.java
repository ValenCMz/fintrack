package com.valencmz.fintrack.service.savinggoal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.valencmz.fintrack.enums.CategoryType;
import com.valencmz.fintrack.enums.TransactionType;
import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.savinggoal.SavingGoalContributionRequest;
import com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest;
import com.valencmz.fintrack.model.dto.savinggoal.SavingGoalResponse;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.model.entity.SavingGoal;
import com.valencmz.fintrack.model.entity.Transaction;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;
import com.valencmz.fintrack.repository.CategoryRepository;
import com.valencmz.fintrack.repository.SavingGoalRepository;
import com.valencmz.fintrack.repository.TransactionRepository;

@Service
public class SavingGoalService {

    private static final String FALLBACK_INCOME_CATEGORY = "Otros ingresos";

    @Autowired
    private SavingGoalRepository savingGoalRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    public List<SavingGoalResponse> getByUser(UserAuth userAuth) {
        return savingGoalRepository.findByUserIdAndActive(userAuth.getUser().getId(), true)
                .stream().map(SavingGoalResponse::new).toList();
    }

    public SavingGoalResponse getById(UUID id, UserAuth userAuth) {
        return new SavingGoalResponse(findOwnedSavingGoal(id, userAuth.getUser().getId()));
    }

    public SavingGoalResponse create(SavingGoalRequest request, UserAuth userAuth) {
        SavingGoal savingGoal = request.toEntity();
        savingGoal.setUser(userAuth.getUser());
        return new SavingGoalResponse(savingGoalRepository.save(savingGoal));
    }

    public SavingGoalResponse update(UUID id, SavingGoalRequest request, UserAuth userAuth) {
        SavingGoal savingGoal = findOwnedSavingGoal(id, userAuth.getUser().getId());
        savingGoal.setName(request.getName());
        savingGoal.setTargetAmount(request.getTargetAmount());
        savingGoal.setCurrentAmount(request.getCurrentAmount());
        savingGoal.setTargetDate(request.getTargetDate());
        savingGoal.setActive(request.isActive());
        return new SavingGoalResponse(savingGoalRepository.save(savingGoal));
    }

    public void softDelete(UUID id, UserAuth userAuth) {
        SavingGoal savingGoal = findOwnedSavingGoal(id, userAuth.getUser().getId());
        savingGoal.setActive(false);
        savingGoalRepository.save(savingGoal);
    }

    /**
     * Registra un aporte a la meta como un ingreso real desde una cuenta, y suma
     * el monto al progreso de la meta en la misma transacción. Si algo falla,
     * no se guarda ni el movimiento ni el avance.
     */
    @Transactional
    public SavingGoalResponse contribute(UUID id, SavingGoalContributionRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        SavingGoal savingGoal = findOwnedSavingGoal(id, userId);

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND));
        Category category = resolveIncomeCategory(request.getCategoryId(), userId);

        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.INCOME);
        transaction.setDescription("Aporte a " + savingGoal.getName());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setNotes("Contribución a meta de ahorro");
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setCard(null);
        transaction.setUser(userAuth.getUser());
        transactionRepository.save(transaction);

        BigDecimal newAmount = savingGoal.getCurrentAmount().add(request.getAmount());
        savingGoal.setCurrentAmount(newAmount);
        return new SavingGoalResponse(savingGoalRepository.save(savingGoal));
    }

    private Category resolveIncomeCategory(UUID categoryId, UUID userId) {
        if (categoryId != null) {
            return categoryRepository.findVisibleById(categoryId, userId)
                    .filter(c -> c.getType() == CategoryType.INCOME)
                    .orElseThrow(() -> new CustomAppException("Income category not found", HttpStatus.NOT_FOUND));
        }
        return categoryRepository.findFirstByNameAndUserIsNullAndType(FALLBACK_INCOME_CATEGORY, CategoryType.INCOME)
                .orElseThrow(() -> new CustomAppException("Income category not found", HttpStatus.NOT_FOUND));
    }

    private SavingGoal findOwnedSavingGoal(UUID id, UUID userId) {
        return savingGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomAppException("Saving goal not found", HttpStatus.NOT_FOUND));
    }
}
