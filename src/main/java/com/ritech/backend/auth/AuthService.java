package com.ritech.backend.auth;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ritech.backend.audit.AuditAction;
import com.ritech.backend.audit.AuditService;
import com.ritech.backend.common.ApiException;
import com.ritech.backend.common.DateTimes;
import com.ritech.backend.common.FieldErrorDto;
import com.ritech.backend.user.User;
import com.ritech.backend.user.UserRepository;

@Service
public class AuthService {

    public static final String RECOVERY_MESSAGE =
            "Si el correo tiene una cuenta, recibirá un enlace para crear una contraseña nueva.";

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmailIgnoreCase(request.email());
        if (found.isEmpty()) {
            log.warn("Failed login for unknown email {}", request.email());
            throw invalidCredentials("email", "EMAIL_NOT_FOUND", "No encontramos una cuenta con este correo.");
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditService.record(user.getId(), AuditAction.LOGIN_FAILED, "Contraseña incorrecta");
            throw invalidCredentials("password", "WRONG_PASSWORD", "La contraseña es incorrecta.");
        }
        JwtService.IssuedToken token = jwtService.issue(user);
        auditService.record(user.getId(), AuditAction.LOGIN_SUCCESS, "Inicio de sesión exitoso");
        return new LoginResponse(token.token(), DateTimes.toBogota(token.expiresAt()), UserDto.from(user));
    }

    public void requestPasswordRecovery(PasswordRecoveryRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresentOrElse(
                user -> auditService.record(user.getId(), AuditAction.PASSWORD_RECOVERY_REQUESTED,
                        "Solicitud de recuperación de contraseña (envío de correo fuera de alcance)"),
                () -> log.info("Password recovery requested for an email without an account"));
    }

    public void logout(Integer userId) {
        auditService.record(userId, AuditAction.LOGOUT, "Cierre de sesión");
    }

    public UserDto currentUser(Integer userId) {
        return userRepository.findById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Debes iniciar sesión para continuar."));
    }

    private static ApiException invalidCredentials(String field, String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "El correo o la contraseña no son correctos.", List.of(new FieldErrorDto(field, code, message)));
    }
}
