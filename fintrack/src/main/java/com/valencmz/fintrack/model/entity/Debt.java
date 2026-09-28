package com.valencmz.fintrack.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.valencmz.fintrack.enums.DebtStatus;

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
public class Debt {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "creditor", nullable = false)
    private String creditor;

    @Column(name = "totalAmount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "remainingAmount", nullable = false)
    private BigDecimal remainingAmount;

    @Column(name = "startDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "status", nullable = false)
    private DebtStatus status;

    // Relationships
    // user
    // a user can have many debts, but a debt can belong to only one user.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
