package com.valencmz.fintrack.service.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.valencmz.fintrack.enums.TransactionType;
import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.transaction.TransactionRequest;
import com.valencmz.fintrack.model.dto.transaction.TransactionResponse;
import com.valencmz.fintrack.model.dto.transaction.TransactionSummaryResponse;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.Card;
import com.valencmz.fintrack.model.entity.CardInstallment;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.model.entity.Transaction;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;
import com.valencmz.fintrack.repository.CardPaymentRepository;
import com.valencmz.fintrack.repository.CardRepository;
import com.valencmz.fintrack.repository.CategoryRepository;
import com.valencmz.fintrack.repository.TransactionRepository;
import com.valencmz.fintrack.service.card.CardService;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CardPaymentRepository cardPaymentRepository;

    public List<TransactionResponse> getByUser(UserAuth userAuth) {
        return transactionRepository.findByUserIdOrderByDateDesc(userAuth.getUser().getId())
                .stream().map(TransactionResponse::new).toList();
    }

    public TransactionResponse getById(UUID id, UserAuth userAuth) {
        return new TransactionResponse(findOwnedTransaction(id, userAuth.getUser().getId()));
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        Category category = categoryRepository.findVisibleById(request.getCategoryId(), userId)
                .orElseThrow(() -> new CustomAppException("Category not found", HttpStatus.NOT_FOUND));

        Transaction transaction = new Transaction();
        applyTarget(request, transaction, userId);
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());
        transaction.setCategory(category);
        transaction.setUser(userAuth.getUser());
        transaction.setInstallments(request.resolvedInstallments());
        if (transaction.getCard() != null) {
            generateInstallments(transaction);
        }
        return new TransactionResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        Transaction transaction = findOwnedTransaction(id, userId);
        Category category = categoryRepository.findVisibleById(request.getCategoryId(), userId)
                .orElseThrow(() -> new CustomAppException("Category not found", HttpStatus.NOT_FOUND));

        boolean isCard = transaction.getCard() != null;
        int previousInstallments = transaction.getInstallments();
        BigDecimal previousAmount = transaction.getAmount();

        applyTarget(request, transaction, userId);
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());
        transaction.setCategory(category);
        syncInstallments(transaction, isCard, previousInstallments, previousAmount, request.resolvedInstallments());
        return new TransactionResponse(transactionRepository.save(transaction));
    }

    /**
     * Una transaccion se paga desde una cuenta o se carga a una tarjeta de
     * credito (que no toca la cuenta hasta que se paga el resumen), pero nunca
     * las dos cosas a la vez.
     */
    private void applyTarget(TransactionRequest request, Transaction transaction, UUID userId) {
        if (request.getCardId() != null) {
            if (request.getType() != TransactionType.EXPENSE) {
                throw new CustomAppException("Solo los gastos se pueden cargar a una tarjeta",
                        HttpStatus.BAD_REQUEST);
            }
            Card card = cardRepository.findByIdAndUserId(request.getCardId(), userId)
                    .orElseThrow(() -> new CustomAppException("Card not found", HttpStatus.NOT_FOUND));
            transaction.setCard(card);
            transaction.setAccount(null);
            return;
        }

        if (request.getAccountId() == null) {
            throw new CustomAppException("Seleccioná una cuenta o una tarjeta", HttpStatus.BAD_REQUEST);
        }
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND));
        transaction.setAccount(account);
        transaction.setCard(null);
    }

    /**
     * Las cuotas solo se pueden rearmar mientras ninguna este paga: una vez que
     * hay plata devuelta, cambiar el plan dejaria los pagos sin respaldo.
     */
    private void syncInstallments(Transaction transaction, boolean wasCard, int previousInstallments,
            BigDecimal previousAmount, int requestedInstallments) {
        List<CardInstallment> current = transaction.getCardInstallments();
        boolean hasPaid = current.stream().anyMatch(CardInstallment::isPaid);
        boolean isCard = transaction.getCard() != null;

        if (!isCard) {
            if (hasPaid) {
                throw new CustomAppException("No se puede quitar la tarjeta de una compra con cuotas pagas",
                        HttpStatus.BAD_REQUEST);
            }
            current.clear();
            transaction.setInstallments(1);
            return;
        }

        boolean changesPlan = !wasCard
                || requestedInstallments != previousInstallments
                || transaction.getAmount().compareTo(previousAmount) != 0
                || current.size() != requestedInstallments;
        if (!changesPlan) {
            transaction.setInstallments(requestedInstallments);
            return;
        }
        if (hasPaid) {
            throw new CustomAppException("No se puede modificar una compra con cuotas pagas",
                    HttpStatus.BAD_REQUEST);
        }
        current.clear();
        transaction.setInstallments(requestedInstallments);
        generateInstallments(transaction);
    }

    /**
     * Reparte el total en cuotas iguales y le carga el resto a la ultima, para
     * que la suma de las cuotas sea siempre exactamente el monto de la compra.
     */
    private void generateInstallments(Transaction transaction) {
        Card card = transaction.getCard();
        int count = transaction.getInstallments();
        BigDecimal total = transaction.getAmount();
        BigDecimal base = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal last = total.subtract(base.multiply(BigDecimal.valueOf(count - 1)));

        for (int number = 1; number <= count; number++) {
            CardInstallment installment = new CardInstallment();
            installment.setNumber(number);
            installment.setAmount(number == count ? last : base);
            installment.setDueDate(CardService.installmentDueDate(card.getDueDayOfMonth(), transaction.getDate(), number));
            installment.setPaid(false);
            installment.setTransaction(transaction);
            installment.setCard(card);
            transaction.getCardInstallments().add(installment);
        }
    }

    @Transactional
    public void delete(UUID id, UserAuth userAuth) {
        Transaction transaction = findOwnedTransaction(id, userAuth.getUser().getId());
        if (transaction.getCard() != null) {
            cardPaymentRepository.deleteByInstallmentTransactionId(id);
        }
        transactionRepository.delete(transaction);
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
