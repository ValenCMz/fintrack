package com.valencmz.fintrack.model.entity;

import java.util.UUID;

import com.valencmz.fintrack.enums.AccountType;

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

/*
 * Donde vive la plata. Es el contenedor/lugar fisico o logico donde entra y
 * sale el dinero
 */
public class Account {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private AccountType type;

    // Opcional a proposito: la cuenta propia no tiene un "titular" que
    // completar. Antes era NOT NULL y el POST /accounts devolvia 409 cuando el
    // cliente no lo mandaba, que es el caso normal de una billetera propia.
    @Column(name = "owner")
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
