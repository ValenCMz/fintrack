package com.valencmz.fintrack.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pago de una cuota de la tarjeta. Mueve dinero de una cuenta real hacia la
 * tarjeta, pero no es un gasto nuevo: las compras ya se registraron como
 * egresos cuando se hicieron. Por eso vive en su propia tabla y no ensucia
 * los resumenes de gastos.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardPayment {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    // Relationships
    // card
    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    // la cuota que este pago salda.
    @ManyToOne
    @JoinColumn(name = "card_installment_id", nullable = false)
    private CardInstallment cardInstallment;

    // account
    // la cuenta desde la que se paga la cuota.
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
