package com.ritech.backend.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.ritech.backend.alert.Alert;
import com.ritech.backend.alert.AlertRepository;
import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.audit.AuditAction;
import com.ritech.backend.audit.AuditService;
import com.ritech.backend.common.ApiException;

class SimulationServiceTest {

    private final SimulationRepository simulationRepository = mock(SimulationRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T15:00:00Z"), ZoneOffset.UTC);
    private final SimulationService service = new SimulationService(simulationRepository, alertRepository,
            auditService, clock);

    @Test
    @DisplayName("SCRUM-23 CA-1: a valid simulation is saved, audited and carries the recommendation in effect")
    void savesSimulation() {
        when(simulationRepository.save(any())).thenAnswer(invocation -> {
            Simulation s = invocation.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 10);
            return s;
        });
        Alert alert = new Alert(1, Recommendation.BUY, "sobreventa", new BigDecimal("28.10"), "Explicación",
                LocalDateTime.parse("2026-10-23T14:55:00"), "RSI14 Wilder 30/70");
        when(alertRepository.findFirstByEmittedAtLessThanEqualOrderByEmittedAtDescIdDesc(
                LocalDateTime.parse("2026-10-23T15:00:00"))).thenReturn(Optional.of(alert));

        SimulationResponse response = service.create(5, new SimulationRequest("1000000", "2"));

        assertThat(response.id()).isEqualTo(10);
        assertThat(response.saved()).isTrue();
        assertThat(response.finalValue()).isEqualByComparingTo("1040000.00");
        assertThat(response.currency()).isEqualTo("COP");
        assertThat(response.createdAt().toString()).isEqualTo("2026-10-23T10:00-05:00");
        assertThat(response.recommendationAtSimulation().type()).isEqualTo(Recommendation.BUY);
        verify(auditService).record(eq(5), eq(AuditAction.SIMULATION_CREATED), anyString());
    }

    @Test
    @DisplayName("SCRUM-23 CA-4: if saving fails the result is still returned with saved=false and a warning")
    void saveFailure() {
        when(simulationRepository.save(any())).thenThrow(new DataAccessResourceFailureException("down"));

        SimulationResponse response = service.create(5, new SimulationRequest("1000000", "-60"));

        assertThat(response.saved()).isFalse();
        assertThat(response.warning()).isEqualTo(SimulationService.NOT_SAVED_WARNING);
        assertThat(response.capped()).isTrue();
        assertThat(response.finalValue()).isEqualByComparingTo("0.00");
        verify(auditService, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("SCRUM-111 CA-04: a simulation of another user is reported as not found")
    void otherUsersSimulation() {
        when(simulationRepository.findByIdAndUserId(3, 5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(5, 3))
                .isInstanceOf(ApiException.class)
                .hasMessage("No se encontró la simulación.");
    }
}
