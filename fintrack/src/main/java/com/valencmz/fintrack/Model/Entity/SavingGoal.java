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
public class SavingGoal {

    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "targetAmount", nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "currentAmount", nullable = false)
    private BigDecimal currentAmount;

    @Column(name = "targetDate", nullable = false)
    private LocalDate targetDate;

    @Column(name = "active", nullable = false)
    private boolean active;

    // Relationships
    // user
    // a user can have many saving goals, but a saving goal can belong to only one
    // user.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
