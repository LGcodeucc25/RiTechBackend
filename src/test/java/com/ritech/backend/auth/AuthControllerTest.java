package com.ritech.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ritech.backend.common.ApiException;
import com.ritech.backend.common.FieldErrorDto;
import com.ritech.backend.config.SecurityConfig;
import com.ritech.backend.user.Role;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("SCRUM-155 CA-1: valid credentials return a token and the user")
    void loginSuccess() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("token-value",
                OffsetDateTime.parse("2026-10-25T18:00:00-05:00"),
                new UserDto(1, "Camila Rojas", "usuario@ritech.co", Role.USER)));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usuario@ritech.co\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-value"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    @DisplayName("SCRUM-155 CA-2: a wrong password returns 401 with a field-level error")
    void loginWrongPassword() throws Exception {
        when(authService.login(any())).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "El correo o la contraseña no son correctos.",
                List.of(new FieldErrorDto("password", "WRONG_PASSWORD", "La contraseña es incorrecta."))));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usuario@ritech.co\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    @DisplayName("SCRUM-155 CA-3: email and password are required and the email format is validated")
    void loginValidation() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    @DisplayName("Password recovery always returns 202 with the generic message")
    void passwordRecovery() throws Exception {
        mockMvc.perform(post("/api/auth/password-recovery").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@ritech.co\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(AuthService.RECOVERY_MESSAGE));
    }

    @Test
    @DisplayName("SCRUM-155 CA-5: protected endpoints reject requests without a token")
    void meRequiresToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Logout is recorded and returns 204")
    void logout() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(jwt().jwt(j -> j.subject("7"))))
                .andExpect(status().isNoContent());
        verify(authService).logout(7);
    }
}
