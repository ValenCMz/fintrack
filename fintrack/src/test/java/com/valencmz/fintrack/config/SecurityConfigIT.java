package com.valencmz.fintrack.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.valencmz.fintrack.controller.AccountController;
import com.valencmz.fintrack.controller.UserController;
import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.RefreshTokenRepository;
import com.valencmz.fintrack.repository.UserRepository;
import com.valencmz.fintrack.service.JwtService;
import com.valencmz.fintrack.service.UsuarioDetailService;
import com.valencmz.fintrack.service.UsuarioService;
import com.valencmz.fintrack.service.account.AccountService;

/**
 * Levanta SecurityConfig + JwtFilter con MockMvc: sin base de datos y sin
 * Docker. El plan original lo llamaba SecurityConfigIT y verificaba a mano con
 * curl; aca queda protegido por un test la cadena completa: filtro → metodo
 * security → PreAuthorize.
 */
@WebMvcTest(controllers = { UserController.class, AccountController.class })
@Import({ SecurityConfig.class, JwtFilter.class, JwtService.class })
@TestPropertySource(properties = {
        "jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LXRlc3Rpbmc=",
        "jwt.expiration=3600000",
        "app.cookie.secure=false",
        "frontend.url=http://localhost:3000" })
@DisplayName("SecurityConfig: 401 sin token, 403 con rol insuficiente")
class SecurityConfigIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private AccountService accountService;
    @MockitoBean
    private UsuarioDetailService usuarioDetailService;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        when(accountService.getByUser(any())).thenReturn(List.of());
        when(usuarioService.getAllUsers()).thenReturn(List.of());
    }

    @Test
    @DisplayName("un endpoint protegido sin token da 401")
    void sinTokenDa401() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token basura o malformado da 401")
    void tokenBasuraDa401() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token no firmado con nuestra clave da 401")
    void tokenDeOtraClaveDa401() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4QHQuY29tIn0.firma-invalida"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un USER no puede listar usuarios: 403")
    void userEnUsersDa403() throws Exception {
        String token = token("USER");
        userConRol("usuario-USER@test.com", Rol.USER);

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un ADMIN sí puede listar usuarios: 200")
    void adminEnUsersDa200() throws Exception {
        String token = token("ADMIN");
        userConRol("usuario-ADMIN@test.com", Rol.ADMIN);

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un USER accede a un recurso propio: 200")
    void userEnAccountsDa200() throws Exception {
        String token = token("USER");
        userConRol("usuario-USER@test.com", Rol.USER);

        mockMvc.perform(get("/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String token(String rol) {
        return jwtService.generateAccessToken("usuario-" + rol + "@test.com", rol);
    }

    private void userConRol(String email, Rol rol) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRol(rol);
        when(usuarioDetailService.loadUserByUsername(email)).thenReturn(new UserAuth(user));
    }
}