package com.valencmz.fintrack.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.auth.ForgotPasswordDTO;
import com.valencmz.fintrack.model.dto.auth.LoginDTO;
import com.valencmz.fintrack.model.dto.auth.LoginResponseDTO;
import com.valencmz.fintrack.model.dto.auth.RegisterDTO;
import com.valencmz.fintrack.model.dto.auth.RegisterDTOI;
import com.valencmz.fintrack.model.dto.auth.RegisterReponseDTO;
import com.valencmz.fintrack.model.dto.auth.ResetPasswordRequestDTO;
import com.valencmz.fintrack.model.dto.user.UserDTO;
import com.valencmz.fintrack.model.dto.user.UsuarioUpdateDTO;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.PasswordResetToken;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.PasswordResetTokenRepository;
import com.valencmz.fintrack.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Service
public class UsuarioService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResponseDTO login(LoginDTO loginDTO, HttpServletResponse response) {

        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword()));

        if (authentication.isAuthenticated()) {
            User usuario = this.userRepository.findByEmail(loginDTO.getEmail());

            if (usuario == null) {
                throw new BadCredentialsException("Credenciales invalidas");
            }

            jwtService.deleteAllRefreshTokensForUser(usuario);

            String accessToken = jwtService.generateAccessToken(usuario.getEmail(), usuario.getRol().name());

            jwtService.generateRefreshToken(usuario, response);

            return new LoginResponseDTO(accessToken);
        } else {
            throw new BadCredentialsException("Credenciales inválidas");
        }
    }

    @Transactional
    public RegisterReponseDTO register(RegisterDTO registerDTO) {
        validateRegister(registerDTO);
        try {
            userRepository.save(
                    new User(
                            registerDTO.getUsername(),
                            registerDTO.getEmail(),
                            passwordEncoder.encode(registerDTO.getPassword()),
                            Rol.USER));

            return new RegisterReponseDTO(registerDTO.getUsername(), registerDTO.getEmail(), Rol.USER);
        } catch (Exception e) {
            throw new CustomAppException("Error al registrar usuario", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateRegister(RegisterDTOI dto) {
        if (!EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
            throw new CustomAppException("Formato de email inválido", HttpStatus.BAD_REQUEST);
        }

        if (this.userRepository.existsByEmail(dto.getEmail().toLowerCase())) {
            throw new CustomAppException("Ya existe un usuario con ese email", HttpStatus.BAD_REQUEST);
        }
    }

    public UserDTO getUserInfo(UserAuth usuarioAuth) {
        User usuario = userRepository.findByEmail(usuarioAuth.getUsername());
        if (usuario == null)
            throw new CustomAppException("Usuario no encontrado", HttpStatus.NOT_FOUND);
        return new UserDTO(usuario);
    }

    public List<UserDTO> getAllUsers() {
        List<User> usuarios = userRepository.findAll();
        return usuarios.stream().map(UserDTO::new).collect(Collectors.toList());
    }

    public void forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        if (!EMAIL_PATTERN.matcher(forgotPasswordDTO.getEmail()).matches()) {
            throw new CustomAppException("Datos mal enviados", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.findByEmail(forgotPasswordDTO.getEmail()) == null) {
            throw new CustomAppException("Error al recuperar contraseña: " + forgotPasswordDTO.getEmail(),
                    HttpStatus.BAD_REQUEST);
        }

        String resetToken = jwtService.generateResetToken(forgotPasswordDTO.getEmail());

        PasswordResetToken passwordResetToken = new PasswordResetToken(
                resetToken,
                forgotPasswordDTO.getEmail(),
                System.currentTimeMillis() + 16 * 60 * 1000);
        this.passwordResetTokenRepository.save(passwordResetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        Resource tpl = new ClassPathResource("templates/password-reset-email.html");
        String template;
        try {
            template = new String(FileCopyUtils.copyToByteArray(tpl.getInputStream()), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new CustomAppException("Error al mandar mail, intente mas tarde", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String html = template.replace("{{RESET_LINK}}", resetLink);

        emailService.sendHtmlEmailAsync(forgotPasswordDTO.getEmail(),
                "Restablece tu contraseña en Sistema de Gestión de Salas", html);
    }

    public void resetPassword(ResetPasswordRequestDTO resetPasswordRequest) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(
                resetPasswordRequest.getResetToken());
        if (passwordResetToken == null ||
                passwordResetToken.isExpired() ||
                !Objects.equals(
                        jwtService.extractEmail(resetPasswordRequest.getResetToken()),
                        passwordResetToken.getEmail())) {
            throw new CustomAppException("Error en la validacion", HttpStatus.BAD_REQUEST);
        }

        User usuario = userRepository.findByEmail(passwordResetToken.getEmail());
        if (usuario == null) {
            throw new CustomAppException("Error al resetear contraseña", HttpStatus.BAD_REQUEST);
        }

        validatePassword(resetPasswordRequest.getNewPassword());

        jwtService.revokeAllRefreshTokensForUser(usuario);

        usuario.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        userRepository.save(usuario);

        passwordResetToken.setUsed(true);
        passwordResetTokenRepository.save(passwordResetToken);
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new CustomAppException("La contraseña es requerida", HttpStatus.BAD_REQUEST);
        }
        if (password.length() < 8) {
            throw new CustomAppException("La contraseña debe tener al menos 8 caracteres",
                    HttpStatus.BAD_REQUEST);
        }
        if (!password.matches(".*[A-Z].*") && !password.matches(".*[0-9].*")) {
            throw new CustomAppException(
                    "La contraseña debe tener al menos una letra mayúscula o un número",
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public void updateUser(UUID id, UsuarioUpdateDTO usuarioDTO) {
        if (id == null)
            throw new CustomAppException("El id es requerido", HttpStatus.BAD_REQUEST);

        User usuario = userRepository.searchById(id)
                .orElseThrow(() -> new CustomAppException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        if (usuarioDTO.getUsername() != null) {
            usuario.setUsername(usuarioDTO.getUsername());
        }

        if (usuarioDTO.getEmail() != null) {
            usuario.setEmail(usuarioDTO.getEmail());
        }
        if (usuarioDTO.getRol() != null) {
            usuario.setRol(usuarioDTO.getRol());
        }

        User saved = userRepository.save(usuario);
    }
}
