package com.valencmz.fintrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.auth.ForgotPasswordDTO;
import com.valencmz.fintrack.model.dto.auth.ResetPasswordRequestDTO;
import com.valencmz.fintrack.model.dto.user.UsuarioUpdateDTO;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.PasswordResetToken;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.PasswordResetTokenRepository;
import com.valencmz.fintrack.repository.UserRepository;

@DisplayName("UsuarioService: actualización PATCH y forgot-password")
class UsuarioServiceTest {

    private UserRepository userRepository;
    private JwtService jwtService;
    private EmailService emailService;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        emailService = mock(EmailService.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);

        usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "userRepository", userRepository);
        ReflectionTestUtils.setField(usuarioService, "jwtService", jwtService);
        ReflectionTestUtils.setField(usuarioService, "emailService", emailService);
        ReflectionTestUtils.setField(usuarioService, "passwordResetTokenRepository", passwordResetTokenRepository);
        passwordEncoder = mock(PasswordEncoder.class);
        ReflectionTestUtils.setField(usuarioService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(usuarioService, "authManager", mock(AuthenticationManager.class));
        ReflectionTestUtils.setField(usuarioService, "frontendUrl", "http://localhost:3000");
    }

    private User user(String email, Rol rol) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("valentin");
        u.setEmail(email);
        u.setPassword("hash");
        u.setRol(rol);
        return u;
    }

    private UserAuth caller(User user) {
        return new UserAuth(user);
    }

    @Nested
    @DisplayName("updateUser: semántica PATCH")
    class PatchSemantica {

        @Test
        @DisplayName("los campos null no pisan los valores existentes")
        void losNullNoPisan() {
            User target = user("yo@test.com", Rol.USER);
            when(userRepository.searchById(target.getId())).thenReturn(Optional.of(target));

            usuarioService.updateUser(target.getId(), new UsuarioUpdateDTO(null, null, null), caller(target));

            assertThat(target.getUsername()).isEqualTo("valentin");
            assertThat(target.getEmail()).isEqualTo("yo@test.com");
            assertThat(target.getRol()).isEqualTo(Rol.USER);
        }

        @Test
        @DisplayName("actualiza solo el username cuando solo viene username")
        void actualizaSoloUsername() {
            User target = user("yo@test.com", Rol.USER);
            when(userRepository.searchById(target.getId())).thenReturn(Optional.of(target));

            usuarioService.updateUser(target.getId(), new UsuarioUpdateDTO("nuevo", null, null), caller(target));

            assertThat(target.getUsername()).isEqualTo("nuevo");
            assertThat(target.getEmail()).isEqualTo("yo@test.com");
        }

        @Test
        @DisplayName("un email inválido da 400")
        void emailInvalidoDa400() {
            User target = user("yo@test.com", Rol.USER);
            when(userRepository.searchById(target.getId())).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> usuarioService.updateUser(
                    target.getId(), new UsuarioUpdateDTO(null, "no-es-email", null), caller(target)))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("un email ya usado por otro da 400")
        void emailDuplicadoDa400() {
            User target = user("yo@test.com", Rol.USER);
            when(userRepository.searchById(target.getId())).thenReturn(Optional.of(target));
            when(userRepository.existsByEmail("otro@test.com")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.updateUser(
                    target.getId(), new UsuarioUpdateDTO(null, "otro@test.com", null), caller(target)))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("guardar el mismo email propio no dispara el chequeo de duplicado")
        void elMismoEmailPropioNoEsDuplicado() {
            User target = user("yo@test.com", Rol.USER);
            when(userRepository.searchById(target.getId())).thenReturn(Optional.of(target));
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            usuarioService.updateUser(target.getId(),
                    new UsuarioUpdateDTO(null, "yo@test.com", null), caller(target));

            verify(userRepository).save(target);
        }

        @Test
        @DisplayName("username vacío se guarda: no hay validación. Inconsistencia conocida")
        void usernameVacioSeGuarda() {
            User target = user("yo@test.com", Rol.USER);
            when(userRepository.searchById(target.getId())).thenReturn(Optional.of(target));

            usuarioService.updateUser(target.getId(), new UsuarioUpdateDTO("", null, null), caller(target));

            assertThat(target.getUsername()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUser: ownership y roles")
    class OwnershipYRoles {

        @Test
        @DisplayName("un usuario no puede modificar a otro: 403")
        void noPuedeModificarAOtro() {
            User me = user("yo@test.com", Rol.USER);
            User otro = user("otro@test.com", Rol.USER);

            assertThatThrownBy(() -> usuarioService.updateUser(
                    otro.getId(), new UsuarioUpdateDTO("hack", null, null), caller(me)))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("un USER no puede cambiar su propio rol: 403")
        void userNoPuedeCambiarSuRol() {
            User me = user("yo@test.com", Rol.USER);
            when(userRepository.searchById(me.getId())).thenReturn(Optional.of(me));

            assertThatThrownBy(() -> usuarioService.updateUser(
                    me.getId(), new UsuarioUpdateDTO(null, null, Rol.ADMIN), caller(me)))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("un ADMIN puede cambiar el rol de cualquiera")
        void adminPuedeCambiarElRol() {
            User admin = user("admin@test.com", Rol.ADMIN);
            User target = user("otro@test.com", Rol.USER);
            when(userRepository.searchById(target.getId())).thenReturn(Optional.of(target));

            usuarioService.updateUser(target.getId(), new UsuarioUpdateDTO(null, null, Rol.ADMIN), caller(admin));

            assertThat(target.getRol()).isEqualTo(Rol.ADMIN);
        }

        @Test
        @DisplayName("un id de otro usuario da 403, sin revelar si existe o no")
        void idDeOtroDa403() {
            User me = user("yo@test.com", Rol.USER);
            UUID id = UUID.randomUUID();
            when(userRepository.searchById(id)).thenReturn(Optional.empty());

            // el chequeo de ownership va antes del de existencia: un 403 no confirma
            // ni descarta que el id exista, que es lo que queremos
            assertThatThrownBy(() -> usuarioService.updateUser(
                    id, new UsuarioUpdateDTO("x", null, null), caller(me)))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
            verify(userRepository, never()).searchById(any());
        }

        @Test
        @DisplayName("un id inexistente da 404 cuando quien pregunta es admin")
        void idInexistenteDa404ParaAdmin() {
            User admin = user("admin@test.com", Rol.ADMIN);
            UUID id = UUID.randomUUID();
            when(userRepository.searchById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.updateUser(
                    id, new UsuarioUpdateDTO("x", null, null), caller(admin)))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("un id null da 400")
        void idNullDa400() {
            User me = user("yo@test.com", Rol.USER);

            assertThatThrownBy(() -> usuarioService.updateUser(
                    null, new UsuarioUpdateDTO("x", null, null), caller(me)))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("forgotPassword: no revela si el email existe")
    class ForgotPasswordNoEnumera {

        @Test
        @DisplayName("un email inexistente no manda mail ni guarda token, y tampoco falla")
        void emailInexistenteNoFalla() {
            when(userRepository.findByEmail("nadie@test.com")).thenReturn(null);

            usuarioService.forgotPassword(new ForgotPasswordDTO("nadie@test.com"));

            verify(emailService, never()).sendHtmlEmailAsync(anyString(), anyString(), anyString());
            verify(passwordResetTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("un email existente manda el mail con el link de reset")
        void emailExistenteMandaMail() {
            when(userRepository.findByEmail("yo@test.com")).thenReturn(user("yo@test.com", Rol.USER));
            when(jwtService.generateResetToken("yo@test.com")).thenReturn("reset-123");

            usuarioService.forgotPassword(new ForgotPasswordDTO("yo@test.com"));

            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendHtmlEmailAsync(
                    org.mockito.ArgumentMatchers.eq("yo@test.com"),
                    anyString(),
                    org.mockito.ArgumentMatchers.contains("http://localhost:3000/reset-password?token=reset-123"));
        }

        @Test
        @DisplayName("un email mal formado sí se rechaza: 400")
        void emailMalFormadoDa400() {
            assertThatThrownBy(() -> usuarioService.forgotPassword(new ForgotPasswordDTO("no-es-email")))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
            verify(emailService, never()).sendHtmlEmailAsync(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("resetPassword: el token se puede usar una sola vez")
    class ResetPassword {

        private PasswordResetToken token(String token, String email, boolean used, boolean expirado) {
            PasswordResetToken t = new PasswordResetToken(
                    token, email, expirado ? System.currentTimeMillis() - 60_000 : System.currentTimeMillis() + 60_000);
            t.setUsed(used);
            return t;
        }

        @Test
        @DisplayName("un token ya usado se rechaza: no se puede reutilizar")
        void tokenUsadoSeRechaza() {
            when(passwordResetTokenRepository.findByToken("t")).thenReturn(
                    token("t", "yo@test.com", true, false));
            when(jwtService.extractEmail("t")).thenReturn("yo@test.com");

            assertThatThrownBy(() -> usuarioService.resetPassword(
                    new ResetPasswordRequestDTO("Nueva1234", "t")))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("un token vencido se rechaza")
        void tokenVencidoSeRechaza() {
            when(passwordResetTokenRepository.findByToken("t")).thenReturn(
                    token("t", "yo@test.com", false, true));
            when(jwtService.extractEmail("t")).thenReturn("yo@test.com");

            assertThatThrownBy(() -> usuarioService.resetPassword(
                    new ResetPasswordRequestDTO("Nueva1234", "t")))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("un token cuyo email no coincide con el del JWT se rechaza")
        void tokenDeOtroEmailSeRechaza() {
            when(passwordResetTokenRepository.findByToken("t")).thenReturn(
                    token("t", "otro@test.com", false, false));
            when(jwtService.extractEmail("t")).thenReturn("yo@test.com");

            assertThatThrownBy(() -> usuarioService.resetPassword(
                    new ResetPasswordRequestDTO("Nueva1234", "t")))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("un token valido resetea la contraseña, revoca refreshes y se marca usado")
        void tokenValidoResetea() {
            User target = user("yo@test.com", Rol.USER);
            PasswordResetToken t = token("t", "yo@test.com", false, false);
            when(passwordResetTokenRepository.findByToken("t")).thenReturn(t);
            when(jwtService.extractEmail("t")).thenReturn("yo@test.com");
            when(userRepository.findByEmail("yo@test.com")).thenReturn(target);
            when(passwordEncoder.encode("Nueva1234")).thenReturn("hash-nuevo");

            usuarioService.resetPassword(new ResetPasswordRequestDTO("Nueva1234", "t"));

            assertThat(target.getPassword()).isEqualTo("hash-nuevo");
            assertThat(t.isUsed()).isTrue();
            verify(jwtService).revokeAllRefreshTokensForUser(target);
            verify(userRepository).save(target);
            verify(passwordResetTokenRepository).save(t);
        }
    }
}
