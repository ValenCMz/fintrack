package com.valencmz.fintrack.model.entity;

import java.util.UUID;

import com.valencmz.fintrack.enums.CategoryType;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(generator = "UUID", strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "color", nullable = false)
    private String color;

    @Column(name = "type", nullable = false)
    private CategoryType type;

    @Column(name = "active", nullable = false)
    private boolean active;

    // Relationships
    // user
    // a user can have many categories, but a category can belong to only one
    // user.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
