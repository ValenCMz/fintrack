package com.valencmz.fintrack.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.valencmz.fintrack.enums.TransactionType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "notes", nullable = true)
    private String notes;

    // Cuotas en las que se paga la compra. 1 = compra comun, sin financiacion.
    @Column(name = "installments", nullable = false)
    private int installments = 1;

    // Las cuotas generadas por esta compra, si es un detalle de tarjeta.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardInstallment> cardInstallments = new ArrayList<>();

    // Relationships
    // category
    // una transaccion puede tener una categoria, pero una categoria puede tener
    // muchas transacciones.
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // account
    // una transaccion puede tener una cuenta, pero una cuenta puede tener muchas
    // transacciones. Puede ser null cuando la compra se hizo con una tarjeta de
    // credito y todavia no se pago: recien el pago mueve la cuenta.
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    // card
    // una compra con tarjeta de credito se asocia a la tarjeta y no toca ninguna
    // cuenta hasta que se paga el resumen.
    @ManyToOne
    @JoinColumn(name = "card_id")
    private Card card;

    // user
    // a user can have many transactions, but a transaction can belong to only one
    // user.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}