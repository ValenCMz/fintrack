package com.valencmz.fintrack.service.card;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.card.CardDetailResponse;
import com.valencmz.fintrack.model.dto.card.CardInstallmentPaymentRequest;
import com.valencmz.fintrack.model.dto.card.CardInstallmentResponse;
import com.valencmz.fintrack.model.dto.card.CardPaymentResponse;
import com.valencmz.fintrack.model.dto.card.CardRequest;
import com.valencmz.fintrack.model.dto.card.CardResponse;
import com.valencmz.fintrack.model.dto.transaction.TransactionResponse;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.Card;
import com.valencmz.fintrack.model.entity.CardInstallment;
import com.valencmz.fintrack.model.entity.CardPayment;
import com.valencmz.fintrack.model.entity.Transaction;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;
import com.valencmz.fintrack.repository.CardInstallmentRepository;
import com.valencmz.fintrack.repository.CardPaymentRepository;
import com.valencmz.fintrack.repository.CardRepository;
import com.valencmz.fintrack.repository.TransactionRepository;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CardInstallmentRepository cardInstallmentRepository;
    @Autowired
    private CardPaymentRepository cardPaymentRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;

    public List<CardResponse> getByUser(UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        return cardRepository.findByUserIdAndActive(userId, true)
                .stream()
                .map(card -> toResponse(card, userId))
                .toList();
    }

    public CardResponse getById(UUID id, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        return toResponse(findOwnedCard(id, userId), userId);
    }

    public CardDetailResponse getDetail(UUID id, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        Card card = findOwnedCard(id, userId);

        List<TransactionResponse> purchases = transactionRepository.findByCardIdAndUserId(id, userId)
                .stream().map(TransactionResponse::new).toList();
        List<CardInstallmentResponse> installments = cardInstallmentRepository.findByCardIdOrderByDueDateAsc(id)
                .stream().map(CardInstallmentResponse::new).toList();
        List<CardPaymentResponse> payments = cardPaymentRepository.findByCardIdAndUserId(id, userId)
                .stream().map(CardPaymentResponse::new).toList();
        CardInstallment next = nextUnpaidInstallment(id);

        return new CardDetailResponse(
                card.getId(),
                card.getHolderName(),
                card.getDueDayOfMonth(),
                nextDueDate(card.getDueDayOfMonth(), LocalDate.now()),
                card.isActive(),
                outstanding(id, userId),
                next != null ? next.getAmount() : null,
                next != null ? next.getDueDate() : null,
                purchases,
                installments,
                payments);
    }

    public CardResponse create(CardRequest request, UserAuth userAuth) {
        Card card = request.toEntity();
        card.setUser(userAuth.getUser());
        return toResponse(cardRepository.save(card), userAuth.getUser().getId());
    }

    public CardResponse update(UUID id, CardRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        Card card = findOwnedCard(id, userId);
        card.setHolderName(request.getHolderName());
        card.setDueDayOfMonth(request.getDueDayOfMonth());
        card.setActive(request.isActive());
        return toResponse(cardRepository.save(card), userId);
    }

    /**
     * Paga una cuota puntual: mueve el importe de la cuenta y marca la cuota
     * como saldada en la misma transaccion.
     */
    @Transactional
    public CardPaymentResponse payInstallment(UUID cardId, UUID installmentId,
            CardInstallmentPaymentRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        Card card = findOwnedCard(cardId, userId);
        CardInstallment installment = cardInstallmentRepository
                .findByIdAndCardIdAndCardUserId(installmentId, cardId, userId)
                .orElseThrow(() -> new CustomAppException("Installment not found", HttpStatus.NOT_FOUND));
        if (installment.isPaid()) {
            throw new CustomAppException("La cuota ya está paga", HttpStatus.BAD_REQUEST);
        }
        return settle(card, installment, request, userId);
    }

    /**
     * Paga todas las cuotas impagas de un detalle (una compra de la tarjeta).
     */
    @Transactional
    public List<CardPaymentResponse> payItem(UUID cardId, UUID transactionId,
            CardInstallmentPaymentRequest request, UserAuth userAuth) {
        UUID userId = userAuth.getUser().getId();
        Card card = findOwnedCard(cardId, userId);
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .filter(t -> t.getCard() != null && cardId.equals(t.getCard().getId()))
                .orElseThrow(() -> new CustomAppException("Purchase not found", HttpStatus.NOT_FOUND));

        List<CardInstallment> unpaid = cardInstallmentRepository.findUnpaidByTransactionId(transactionId);
        if (unpaid.isEmpty()) {
            throw new CustomAppException("El detalle ya está saldado", HttpStatus.BAD_REQUEST);
        }
        return unpaid.stream().map(installment -> settle(card, installment, request, userId)).toList();
    }

    private CardPaymentResponse settle(Card card, CardInstallment installment,
            CardInstallmentPaymentRequest request, UUID userId) {
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new CustomAppException("Account not found", HttpStatus.NOT_FOUND));

        installment.setPaid(true);
        installment.setPaidDate(request.getDate());
        installment.setAccount(account);
        cardInstallmentRepository.save(installment);

        CardPayment payment = new CardPayment();
        payment.setCard(card);
        payment.setCardInstallment(installment);
        payment.setAccount(account);
        payment.setAmount(installment.getAmount());
        payment.setDate(request.getDate());
        payment.setUser(card.getUser());
        return new CardPaymentResponse(cardPaymentRepository.save(payment));
    }

    public void softDelete(UUID id, UserAuth userAuth) {
        Card card = findOwnedCard(id, userAuth.getUser().getId());
        card.setActive(false);
        cardRepository.save(card);
    }

    private CardResponse toResponse(Card card, UUID userId) {
        CardInstallment next = nextUnpaidInstallment(card.getId());
        return new CardResponse(
                card,
                nextDueDate(card.getDueDayOfMonth(), LocalDate.now()),
                outstanding(card.getId(), userId),
                next != null ? next.getAmount() : null,
                next != null ? next.getDueDate() : null);
    }

    /**
     * Lo que todavia no se pago: la suma de las cuotas impagas.
     */
    private BigDecimal outstanding(UUID cardId, UUID userId) {
        return cardInstallmentRepository.findByCardIdAndPaidOrderByDueDateAsc(cardId, false)
                .stream()
                .map(CardInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CardInstallment nextUnpaidInstallment(UUID cardId) {
        List<CardInstallment> unpaid = cardInstallmentRepository
                .findByCardIdAndPaidOrderByDueDateAsc(cardId, false);
        return unpaid.isEmpty() ? null : unpaid.get(0);
    }

    /**
     * Proximo vencimiento: el dia del mes pedido en el mes actual, o el del mes
     * que viene si ese dia ya paso. Si el mes no tiene ese dia (ej. 31 en
     * febrero) se usa el ultimo dia del mes.
     */
    static LocalDate nextDueDate(int dayOfMonth, LocalDate from) {
        YearMonth month = YearMonth.from(from);
        LocalDate candidate = month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
        if (candidate.isBefore(from)) {
            month = month.plusMonths(1);
            candidate = month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
        }
        return candidate;
    }

    /**
     * Vencimiento de la cuota n de un detalle: el primer cierre despues de la
     * compra, mas n-1 meses.
     */
    public static LocalDate installmentDueDate(int dayOfMonth, LocalDate purchaseDate, int number) {
        LocalDate firstDue = nextDueDate(dayOfMonth, purchaseDate);
        YearMonth month = YearMonth.from(firstDue).plusMonths(number - 1L);
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }

    private Card findOwnedCard(UUID id, UUID userId) {
        return cardRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomAppException("Card not found", HttpStatus.NOT_FOUND));
    }
}
