package com.valencmz.fintrack.model.entity;

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

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "holderName", nullable = false)
    private String holderName;

    // Dia del mes en que vence el resumen (1-31).
    @Column(name = "dueDayOfMonth", nullable = false)
    private int dueDayOfMonth;

    @Column(name = "active", nullable = false)
    private boolean active;

    // Relationships
    // user
    // a user can have many cards, but a card can belong to only one user.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
