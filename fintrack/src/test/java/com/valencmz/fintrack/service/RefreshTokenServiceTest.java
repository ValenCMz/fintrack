package com.valencmz.fintrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.model.dto.auth.LoginResponseDTO;
import com.valencmz.fintrack.model.entity.RefreshToken;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.repository.RefreshTokenRepository;
import com.valencmz.fintrack.repository.UserRepository;

import io.jsonwebtoken.SignatureException;

@DisplayName("JwtService: la cadena del refresh token")
class RefreshTokenServiceTest {

    private static final String SECRET = "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LXRlc3Rpbmc=";

    private JwtService jwtService;
    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);

        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "cookieSecure", false);
        ReflectionTestUtils.setField(jwtService, "refreshTokenRepository", refreshTokenRepository);
        ReflectionTestUtils.setField(jwtService, "userRepository", userRepository);
        initSigningKey();

        user = new User("valentin", "yo@test.com", "hash", Rol.USER);
    }

    private String hash(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt());
    }

    private RefreshToken candidato(String raw, LocalDateTime expiracion, boolean revocado) {
        RefreshToken token = new RefreshToken(hash(raw), user, expiracion);
        token.setRevoked(revocado);
        return token;
    }

    @Test
    @DisplayName("un refresh token null o vacio se rechaza")
    void nullOVacioSeRechaza() {
        assertThatThrownBy(() -> jwtService.validateRefreshToken(null)).isInstanceOf(SignatureException.class);
        assertThatThrownBy(() -> jwtService.validateRefreshToken("")).isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("un token que no coincide con ningun candidato se rechaza")
    void sinCoincidenciaSeRechaza() {
        when(refreshTokenRepository.findByRevokedFalseAndFechaExpiracionAfter(any()))
                .thenReturn(List.of(candidato("otro-token", LocalDateTime.now().plusDays(10), false)));

        assertThatThrownBy(() -> jwtService.validateRefreshToken("token-desconocido"))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("un refresh token vencido se rechaza aunque el hash coincida")
    void vencidoSeRechaza() {
        when(refreshTokenRepository.findByRevokedFalseAndFechaExpiracionAfter(any()))
                .thenReturn(List.of(candidato("raw", LocalDateTime.now().minusHours(1), false)));

        assertThatThrownBy(() -> jwtService.validateRefreshToken("raw")).isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("un refresh token revocado se rechaza aunque el hash coincida")
    void revocadoSeRechaza() {
        when(refreshTokenRepository.findByRevokedFalseAndFechaExpiracionAfter(any()))
                .thenReturn(List.of(candidato("raw", LocalDateTime.now().plusDays(10), true)));

        assertThatThrownBy(() -> jwtService.validateRefreshToken("raw")).isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("un refresh token valido devuelve la entidad")
    void validoDevuelveLaEntidad() {
        RefreshToken esperado = candidato("raw", LocalDateTime.now().plusDays(10), false);
        when(refreshTokenRepository.findByRevokedFalseAndFechaExpiracionAfter(any()))
                .thenReturn(List.of(esperado));

        assertThat(jwtService.validateRefreshToken("raw")).isSameAs(esperado);
    }

    @Test
    @DisplayName("refresh rota: revoca el viejo, emite uno nuevo y devuelve access token")
    void refreshRotaElToken() {
        RefreshToken old = candidato("raw", LocalDateTime.now().plusDays(10), false);
        when(refreshTokenRepository.findByRevokedFalseAndFechaExpiracionAfter(any()))
                .thenReturn(List.of(old));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        MockHttpServletResponse response = new MockHttpServletResponse();
        LoginResponseDTO result = jwtService.refresh("raw", response);

        // el viejo queda revocado
        assertThat(old.getRevoked()).isTrue();

        // se guardo un refresh nuevo: 1 save por revocar el viejo + 1 por el nuevo
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        RefreshToken nuevo = captor.getAllValues().get(1);
        assertThat(nuevo.getRevoked()).isFalse();

        // el access token nuevo llega en el body
        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(jwtService.extractEmail(result.getAccessToken())).isEqualTo("yo@test.com");

        // la cookie es httpOnly, con path y vida de 30 dias
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
                .startsWith("refreshTokenFinTrack=")
                .contains("HttpOnly")
                .contains("Path=/api/auth/refreshToken")
                .contains("Max-Age=2592000");
        assertThat(nuevo.getFechaExpiracion())
                .isAfter(LocalDateTime.now().plusDays(29))
                .isBefore(LocalDateTime.now().plusDays(31));
    }

    @Test
    @DisplayName("reutilizar un refresh token ya rotado se rechaza")
    void reutilizacionSeRechaza() {
        RefreshToken old = candidato("raw", LocalDateTime.now().plusDays(10), false);
        when(refreshTokenRepository.findByRevokedFalseAndFechaExpiracionAfter(any()))
                .thenReturn(List.of(old), List.of()); // queda revocado despues de la primera

        jwtService.refresh("raw", new MockHttpServletResponse());

        assertThatThrownBy(() -> jwtService.refresh("raw", new MockHttpServletResponse()))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("revokeAllRefreshTokensForUser revoca todos los abiertos")
    void revokeAllRevocaTodos() {
        RefreshToken a = candidato("a", LocalDateTime.now().plusDays(10), false);
        RefreshToken b = candidato("b", LocalDateTime.now().plusDays(10), false);
        when(refreshTokenRepository.findByUsuarioId(user.getId())).thenReturn(List.of(a, b));

        jwtService.revokeAllRefreshTokensForUser(user);

        assertThat(a.getRevoked()).isTrue();
        assertThat(b.getRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    private void initSigningKey() {
        try {
            Method init = JwtService.class.getDeclaredMethod("initSigningKey");
            init.setAccessible(true);
            init.invoke(jwtService);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}