package com.ritech.backend.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.analysis.RecommendationEngine;
import com.ritech.backend.analysis.Volatility;
import com.ritech.backend.common.ApiException;
import com.ritech.backend.market.MarketData;
import com.ritech.backend.market.MarketDataRepository;
import com.ritech.backend.market.MarketService;

class AlertServiceTest {

    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final MarketDataRepository marketDataRepository = mock(MarketDataRepository.class);
    private final MarketService marketService = mock(MarketService.class);
    private final AlertService service = new AlertService(alertRepository, marketDataRepository, marketService);

    @Test
    @DisplayName("SCRUM-152 CA-5: the detail reconstructs the data used, period, rule, emission and receiver")
    void detail() {
        Alert alert = new Alert(42, Recommendation.SELL, "sobrecompra (alta volatilidad)", new BigDecimal("72.06"),
                "El RSI alcanzó 72,06...", LocalDateTime.parse("2026-10-23T13:40:00"), RecommendationEngine.RULE);
        ReflectionTestUtils.setField(alert, "id", 7);
        MarketData data = new MarketData(new BigDecimal("25131.25"), new BigDecimal("0.5250"),
                new BigDecimal("72.0612"), "ABIERTO", LocalDateTime.parse("2026-10-23T13:40:00"));
        ReflectionTestUtils.setField(data, "id", 42);
        when(alertRepository.findById(7)).thenReturn(Optional.of(alert));
        when(marketDataRepository.findById(42)).thenReturn(Optional.of(data));
        when(marketService.pointsForDay(LocalDate.parse("2026-10-23"))).thenReturn(List.of());

        AlertDetail detail = service.detail(7, "Camila Rojas");

        assertThat(detail.emittedAt().toString()).isEqualTo("2026-10-23T08:40-05:00");
        assertThat(detail.emittedAtNewYork().toString()).isEqualTo("2026-10-23T09:40-04:00");
        assertThat(detail.volatilityWarning()).isEqualTo(RecommendationEngine.OPENING_WARNING);
        assertThat(detail.traceability().period()).isEqualTo("14 ventanas de 5 minutos");
        assertThat(detail.traceability().rule()).isEqualTo("RSI14 Wilder 30/70");
        assertThat(detail.traceability().receivedBy()).isEqualTo("Camila Rojas");
        assertThat(detail.traceability().dataUsed().marketDataId()).isEqualTo(42);
        assertThat(detail.traceability().dataUsed().windowStart().toString()).isEqualTo("2026-10-23T08:35-05:00");
        assertThat(detail.criteria()).extracting(AlertDetail.Criterion::met)
                .containsExactly(false, true, false, true, false);
    }

    @Test
    @DisplayName("A missing alert is reported as not found")
    void notFound() {
        when(alertRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(1, null)).isInstanceOf(ApiException.class)
                .hasMessage("No se encontró la alerta.");
    }

    @Test
    @DisplayName("Criteria mark the exact 30.00 limit as BUY")
    void criteriaAtLimit() {
        assertThat(AlertService.criteria(new BigDecimal("30.00"), Volatility.NONE))
                .extracting(AlertDetail.Criterion::met).containsExactly(true, false, false, true, true);
    }

    @Test
    @DisplayName("An invalid type filter is rejected")
    void invalidType() {
        assertThatThrownBy(() -> service.list("BUY,FOO", null, null, null, null)).isInstanceOf(ApiException.class);
    }
}
