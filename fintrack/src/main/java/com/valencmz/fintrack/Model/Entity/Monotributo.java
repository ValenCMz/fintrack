package com.valencmz.fintrack.Model.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.valencmz.fintrack.enums.MonotributoStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Monotributo {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "monthlyAmount", nullable = false)
    private BigDecimal monthlyAmount;

    @Column(name = "dueDay", nullable = false)
    private LocalDate dueDay;

    @Column(name = "status", nullable = false)
    private MonotributoStatus status;

    // Relationships
    // user
    // a user can have many monotributos, but a monotributo can belong to only one
    // user.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
