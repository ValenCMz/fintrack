package com.valencmz.fintrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.UserAuth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@DisplayName("JwtService: la clave de firma y la cadena del access token")
class JwtServiceKeyTest {

    private static final String SECRET = "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LXRlc3Rpbmc=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "cookieSecure", false);
        initSigningKey(jwtService);
    }

    @Test
    @DisplayName("un jwt.secret que no es Base64 falla al arrancar")
    void claveQueNoEsBase64Falla() {
        InvocationTargetException ex = tryInitSigningKey("no-es-base64!!!");

        assertThat(ex).isNotNull();
        Throwable cause = ex.getCause();
        assertThat(cause).isInstanceOf(IllegalStateException.class);
        assertThat(cause.getMessage()).contains("no es un Base64 valido");
    }

    @Test
    @DisplayName("un jwt.secret de menos de 32 bytes falla al arrancar")
    void claveDeMenosDe32BytesFalla() {
        InvocationTargetException ex = tryInitSigningKey("MTIz"); // decodifica a 3 bytes

        assertThat(ex).isNotNull();
        Throwable cause = ex.getCause();
        assertThat(cause).isInstanceOf(IllegalStateException.class);
        assertThat(cause.getMessage()).contains("al menos 32 bytes");
    }

    @Test
    @DisplayName("un jwt.secret valido de 32+ bytes inicializa la clave")
    void claveValidaInicializa() {
        assertThat(tryInitSigningKey(SECRET)).isNull();
    }

    @Test
    @DisplayName("el access token firma con la clave y expira en una hora por defecto")
    void expiraEnUnaHoraPorDefecto() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 3600000L);

        String token = jwtService.generateAccessToken("yo@test.com", "USER");
        Claims claims = parse(token);
        UserAuth userAuth = new UserAuth(user("yo@test.com", com.valencmz.fintrack.enums.Rol.USER));

        assertThat(claims.getSubject()).isEqualTo("yo@test.com");
        assertThat(claims.get("rol", String.class)).isEqualTo("USER");
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofHours(1));
        assertThat(jwtService.validateAccessToken(token, userAuth)).isTrue();
    }

    @Test
    @DisplayName("la expiracion respeta el valor configurado, no la hora fija")
    void expiraConElTiempoConfigurado() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 7000L);

        Claims claims = parse(jwtService.generateAccessToken("yo@test.com", "USER"));

        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofSeconds(7));
    }

    @Test
    @DisplayName("el reset token expira a los 15 minutos")
    void resetTokenExpiraEn15Minutos() {
        Claims claims = parse(jwtService.generateResetToken("yo@test.com"));

        assertThat(claims.getSubject()).isEqualTo("yo@test.com");
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("un token firmado con otra clave se rechaza")
    void tokenConOtraClaveSeRechaza() {
        String token = jwtService.generateAccessToken("yo@test.com", "USER");
        String otraSecret = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("otra-clave-distinta-de-32-bytes-minimo!".getBytes());

        assertThatThrownBy(() -> Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(otraSecret)))
                .build()
                .parseSignedClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("un token manipulado se rechaza al extraer el email")
    void tokenManipuladoSeRechaza() {
        String token = jwtService.generateAccessToken("yo@test.com", "USER");
        String tampered = token.substring(0, token.length() - 1) + "A";

        assertThatThrownBy(() -> jwtService.extractEmail(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("validateAccessToken con otro usuario da false")
    void validateAccessTokenConOtroUsuarioDaFalse() {
        String token = jwtService.generateAccessToken("yo@test.com", "USER");

        UserDetails otro = new UserAuth(user("otro@test.com", com.valencmz.fintrack.enums.Rol.USER));

        assertThat(jwtService.validateAccessToken(token, otro)).isFalse();
    }

    @Test
    @DisplayName("extractRol devuelve el claim del rol")
    void extractRolDevuelveElRol() {
        String token = jwtService.generateAccessToken("yo@test.com", "ADMIN");

        assertThat(jwtService.extractRol(token)).isEqualTo("ADMIN");
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private InvocationTargetException tryInitSigningKey(String secret) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        return initSigningKey(service);
    }

    private InvocationTargetException initSigningKey(JwtService service) {
        try {
            Method init = JwtService.class.getDeclaredMethod("initSigningKey");
            init.setAccessible(true);
            init.invoke(service);
        } catch (InvocationTargetException e) {
            return e;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }

    private User user(String email, com.valencmz.fintrack.enums.Rol rol) {
        User u = new User();
        u.setId(java.util.UUID.randomUUID());
        u.setEmail(email);
        u.setRol(rol);
        return u;
    }
}