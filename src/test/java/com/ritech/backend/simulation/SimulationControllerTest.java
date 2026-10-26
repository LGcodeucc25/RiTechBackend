package com.ritech.backend.simulation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.ritech.backend.common.ApiException;
import com.ritech.backend.config.SecurityConfig;

@WebMvcTest(SimulationController.class)
@Import(SecurityConfig.class)
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimulationService simulationService;

    private static RequestPostProcessor user(int id) {
        return jwt().jwt(j -> j.subject(String.valueOf(id))).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor manager() {
        return jwt().jwt(j -> j.subject("2")).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
    }

    @Test
    @DisplayName("SCRUM-158 CA-4: a manager gets 403 on every simulation endpoint")
    void managerForbidden() throws Exception {
        mockMvc.perform(get("/api/simulations").with(manager())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/simulations/1").with(manager())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/simulations").with(manager()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"capital\":1000,\"variation\":1}")).andExpect(status().isForbidden());
        verifyNoInteractions(simulationService);
    }

    @Test
    @DisplayName("SCRUM-37 CA-1: the list only asks for the authenticated user's simulations")
    void listOwnOnly() throws Exception {
        when(simulationService.list(eq(1), any(), any(), any())).thenReturn(List.of());
        mockMvc.perform(get("/api/simulations").with(user(1))).andExpect(status().isOk());
        verify(simulationService).list(1, null, null, null);
    }

    @Test
    @DisplayName("SCRUM-13 CA-1: validation errors return 400 with codes and Spanish messages")
    void validationError() throws Exception {
        when(simulationService.create(eq(1), any())).thenAnswer(invocation ->
                SimulationValidator.validate(invocation.getArgument(1)));

        mockMvc.perform(post("/api/simulations").with(user(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capital\":-5,\"variation\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("CAPITAL_MUST_BE_POSITIVE"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("El capital debe ser mayor que cero."))
                .andExpect(jsonPath("$.fieldErrors[1].code").value("VARIATION_MUST_BE_NUMERIC"));
    }

    @Test
    @DisplayName("SCRUM-111 CA-04: a missing simulation returns 404 with the Spanish message")
    void notFound() throws Exception {
        when(simulationService.get(1, 99)).thenThrow(ApiException.notFound("No se encontró la simulación."));

        mockMvc.perform(get("/api/simulations/99").with(user(1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró la simulación."));
    }

    @Test
    @DisplayName("SCRUM-155 CA-5: simulations require a token")
    void requiresToken() throws Exception {
        mockMvc.perform(get("/api/simulations")).andExpect(status().isUnauthorized());
    }
}
