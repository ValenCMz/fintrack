package com.valencmz.fintrack.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.valencmz.fintrack.enums.TransactionType;

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

    // Relationships
    // category
    // una transaccion puede tener una categoria, pero una categoria puede tener
    // muchas transacciones.
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // account
    // una transaccion puede tener una cuenta, pero una cuenta puede tener muchas
    // transacciones.
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // user
    // a user can have many transactions, but a transaction can belong to only one
    // user.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}