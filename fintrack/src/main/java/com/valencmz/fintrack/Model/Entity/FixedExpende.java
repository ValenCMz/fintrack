package com.valencmz.fintrack.Model.Entity;

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

@Entity
public class FixedExpende {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "dueDay", nullable = false)
    private LocalDate dueDay;

    @Column(name = "frequency", nullable = true)
    private String frequency;

    @Column(name = "active", nullable = false)
    private boolean active;

    // Relationships
    // category
    // account

    // un gasto fijo tiene una categoria, pero una categoria puede tener muchos
    // gastos fijos.
    @JoinColumn(name = "category_id", nullable = false)
    @ManyToOne
    private Category category;

    // account
    // un gasto fijo tiene una cuenta, pero una cuenta puede tener muchos gastos
    // fijos.
    @JoinColumn(name = "account_id", nullable = false)
    @ManyToOne
    private Account account;

    // user
    // a user can have many fixed expenses, but a fixed expense can belong to only
    // one user.
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne
    private User user;

}
