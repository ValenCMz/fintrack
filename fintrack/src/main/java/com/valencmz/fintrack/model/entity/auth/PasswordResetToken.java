package com.valencmz.fintrack.model.entity.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PasswordResetToken {
    @Id
    private String token;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private Long expirationTime;
    @Column(nullable = false)
    private boolean used = false;

    public PasswordResetToken() {
        // Default constructor for JPA
    }

    public PasswordResetToken(String token, String email, Long expirationTime) {
        this.token = token;
        this.email = email;
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
}
