package com.valencmz.fintrack.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.valencmz.fintrack.model.dto.auth.LoginResponseDTO;
import com.valencmz.fintrack.model.entity.RefreshToken;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.RefreshTokenRepository;
import com.valencmz.fintrack.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    public LoginResponseDTO refresh(String refreshTokenString, HttpServletResponse response) {
        RefreshToken rtEntity = validateRefreshToken(refreshTokenString);
        revokeRefreshToken(rtEntity);
        User usuario = rtEntity.getUser();
        String newAccessToken = generateAccessToken(
                usuario.getEmail(),
                usuario.getRol().name());
        generateRefreshToken(usuario, response);

        return new LoginResponseDTO(newAccessToken);
    }

    public RefreshToken validateRefreshToken(String tokenString) {

        if (tokenString == null || tokenString.isEmpty()) {
            throw new SignatureException("Refresh token is null or empty");
        }

        List<RefreshToken> candidatos = refreshTokenRepository.findByRevokedFalseAndFechaExpiracionAfter(
                LocalDateTime.now());

        // 2) Busco coincidencia de hash contra cada candidato
        for (int i = 0; i < candidatos.size(); i++) {
            RefreshToken candidato = candidatos.get(i);

            boolean matches = BCrypt.checkpw(tokenString, candidato.getHashedToken());

            if (matches) {
                // Si encontramos un candidato que coincide, verificamos las condiciones
                // adicionales
                if (candidato.getRevoked()) {
                    throw new SignatureException("Refresh token revoked");
                }
                if (candidato.getFechaExpiracion().isBefore(LocalDateTime.now())) {
                    throw new SignatureException("Expired refresh token");
                }
                return candidato; // encontrado, devolvemos la entidad completa
            }
        }

        throw new SignatureException("Refresh token inválido o no coincide con deviceId");
    }

    public void generateRefreshToken(User usuario, HttpServletResponse response) {

        String rawToken = generarRandomRefreshToken();

        LocalDateTime fechaExpiracion = LocalDateTime.now().plusDays(30);

        refreshTokenRepository.save(
                new RefreshToken(BCrypt.hashpw(rawToken, BCrypt.gensalt()), usuario, fechaExpiracion));

        ResponseCookie cookie = ResponseCookie.from("refreshTokenFinTrack", rawToken)
                .httpOnly(true)
                .secure(true)
                .path("/auth/refreshToken")
                .maxAge(Duration.ofDays(30))
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String generateAccessToken(String email, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 60000 * 60 * 1)) // 1 hora de expiración
                .and()
                .signWith(getKey())
                .compact();
    }

    public boolean revokeAfterLogout(UserAuth usuarioAuth) {
        User usuario = this.userRepository.findByEmail(usuarioAuth.getUsername());
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        try {
            refreshTokenRepository.revokeAllRefreshTokensForUser(usuario.getId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void revokeAllRefreshTokensForUser(User usuario) {
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUsuarioId(usuario.getId());
        for (RefreshToken token : refreshTokens) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }
    }

    public String generateResetToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 15)) // 15 minutos de expiración
                .signWith(getKey())
                .compact();
    }

    private String generarRandomRefreshToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public void revokeRefreshToken(RefreshToken refreshToken) {
        if (refreshToken != null) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        } else {
            throw new SignatureException("Invalid refresh token");
        }
    }

    // averiguar si falta algo mas
    public boolean validateAccessToken(String accessToken, UserDetails userDetails) {
        final String userName = extractEmail(accessToken);
        // checkear redundancia con lo que hacemos en el jwtfilter
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(accessToken));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractRol(String accessToken) {
        return extractAllClaims(accessToken).get("rol", String.class);
    }

    public String extractEmail(String accessToken) {
        return extractClaim(accessToken, Claims::getSubject);
    }

    private <T> T extractClaim(String accessToken, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(accessToken);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String accessToken) {
        try {
            return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(accessToken).getPayload();
        } catch (ExpiredJwtException e) {
            throw e; // O logueás, o simplemente dejás que se capture arriba
        }
    }

    @Transactional
    public void deleteAllRefreshTokensForUser(User usuario) {
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUsuarioId(usuario.getId());
        for (RefreshToken token : refreshTokens) {
            refreshTokenRepository.delete(token);
        }
    }

}
