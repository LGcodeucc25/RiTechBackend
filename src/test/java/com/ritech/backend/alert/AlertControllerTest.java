package com.ritech.backend.alert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ritech.backend.common.ApiException;
import com.ritech.backend.common.PageResponse;
import com.ritech.backend.config.SecurityConfig;

@WebMvcTest(AlertController.class)
@Import(SecurityConfig.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @Test
    @DisplayName("SCRUM-158: both roles can read alerts")
    void bothRoles() throws Exception {
        when(alertService.list(any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        for (String role : List.of("ROLE_USER", "ROLE_MANAGER")) {
            mockMvc.perform(get("/api/alerts").with(jwt().authorities(new SimpleGrantedAuthority(role))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Test
    @DisplayName("A missing alert returns 404 with a Spanish message")
    void notFound() throws Exception {
        when(alertService.detail(eq(5), any())).thenThrow(ApiException.notFound("No se encontró la alerta."));
        mockMvc.perform(get("/api/alerts/5").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró la alerta."));
    }

    @Test
    @DisplayName("SCRUM-155 CA-5: alerts require a token")
    void requiresToken() throws Exception {
        mockMvc.perform(get("/api/alerts")).andExpect(status().isUnauthorized());
    }
}
