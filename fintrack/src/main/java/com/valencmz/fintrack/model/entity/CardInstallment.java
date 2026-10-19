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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una cuota de un detalle de la tarjeta. Cada compra en cuotas genera N de
 * estos registros, y el saldo de la tarjeta es la suma de las impagas.
 */
@Entity
@Table(name = "card_installment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardInstallment {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid", nullable = false)
    private boolean paid;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    // Relationships
    // la compra que genera la cuota.
    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    // la cuenta desde la que se pago la cuota, si ya se pago.
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
}
