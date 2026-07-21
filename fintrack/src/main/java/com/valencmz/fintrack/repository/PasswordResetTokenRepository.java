package com.valencmz.fintrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.valencmz.fintrack.model.entity.auth.PasswordResetToken;

import jakarta.transaction.Transactional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    PasswordResetToken findByToken(String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.used = true OR p.expirationTime < :now")
    void deleteByExpirationTimeBefore(@Param("now") Long now);
}
