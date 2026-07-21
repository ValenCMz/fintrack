package com.valencmz.fintrack.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.valencmz.fintrack.model.entity.RefreshToken;

import jakarta.transaction.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    List<RefreshToken> findByRevokedFalseAndFechaExpiracionAfter(LocalDateTime ahora);

    @Transactional
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :usuarioId")
    void revokeAllRefreshTokensForUser(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :usuarioId")
    List<RefreshToken> findByUsuarioId(@Param("usuarioId") UUID usuarioId);

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true OR rt.fechaExpiracion < :now")
    int deleteByRevokedTrueOrFechaExpiracionBefore(@Param("now") LocalDateTime now);

}
