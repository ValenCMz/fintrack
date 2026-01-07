package com.valencmz.fintrack.Model.Entity;

import java.util.UUID;

import com.valencmz.fintrack.enums.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Account {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private AccountType type;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "active", nullable = false)
    private boolean active;

    // Relationships
    // user
    // a user has many account, but an account belong to only one user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
