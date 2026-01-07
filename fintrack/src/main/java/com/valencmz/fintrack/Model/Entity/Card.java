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
public class Card {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "holderName", nullable = false)
    private String holderName;

    @Column(name = "dueDay", nullable = false)
    private LocalDate dueDay;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "active", nullable = false)
    private boolean active;

    // Relationships
    // user
    // a user can have many cards, but a card can belong to only one user.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
