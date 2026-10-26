package com.ritech.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.ritech.backend.audit.AuditAction;
import com.ritech.backend.audit.AuditService;
import com.ritech.backend.common.ApiException;
import com.ritech.backend.config.RitechProperties;
import com.ritech.backend.user.RoleEntity;
import com.ritech.backend.user.User;
import com.ritech.backend.user.UserRepository;

class AuthServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-signing";

    private final SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "HmacSHA256");
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    private final RitechProperties properties = new RitechProperties(
            new RitechProperties.Jwt(SECRET, Duration.ofHours(8)), new RitechProperties.Cors(List.of()),
            new RitechProperties.Seed(false, "", ""));
    private final AuthService authService = new AuthService(userRepository, passwordEncoder,
            new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), properties, clock), auditService);

    private User user() {
        User user = new User("Camila Rojas", "usuario@ritech.co", passwordEncoder.encode("right"),
                new RoleEntity("USUARIO"));
        ReflectionTestUtils.setField(user, "id", 1);
        return user;
    }

    @Test
    @DisplayName("SCRUM-155 CA-1: a valid login issues an HS256 token with the expected claims for 8 hours")
    void issuesToken() {
        when(userRepository.findByEmailIgnoreCase("USUARIO@ritech.co")).thenReturn(Optional.of(user()));

        LoginResponse response = authService.login(new LoginRequest("USUARIO@ritech.co", "right"));

        Jwt jwt = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()
                .decode(response.token());
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("usuario@ritech.co");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Camila Rojas");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(8));
        verify(auditService).record(1, AuditAction.LOGIN_SUCCESS, "Inicio de sesión exitoso");
    }

    @Test
    @DisplayName("SCRUM-155 CA-2: a wrong password is audited and reported on the password field")
    void wrongPassword() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> authService.login(new LoginRequest("usuario@ritech.co", "wrong")))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getFieldErrors().getFirst().field()).isEqualTo("password"));
        verify(auditService).record(eq(1), eq(AuditAction.LOGIN_FAILED), anyString());
    }

    @Test
    @DisplayName("SCRUM-155 CA-2: an unknown email is reported on the email field and not audited")
    void unknownEmail() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("x@ritech.co", "any")))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getFieldErrors().getFirst().field()).isEqualTo("email"));
        verifyNoInteractions(auditService);
    }
}
