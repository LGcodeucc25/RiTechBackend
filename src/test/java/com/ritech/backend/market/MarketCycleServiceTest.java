package com.ritech.backend.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.ritech.backend.alert.Alert;
import com.ritech.backend.alert.AlertRepository;
import com.ritech.backend.analysis.RecommendationEngine;
import com.ritech.backend.integration.IntegrationLogService;

class MarketCycleServiceTest {

    private final YahooFinanceClient client = mock(YahooFinanceClient.class);
    private final MarketDataRepository marketDataRepository = mock(MarketDataRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final IntegrationLogService integrationLogService = mock(IntegrationLogService.class);
    private final TransactionTemplate transactionTemplate =
            new TransactionTemplate(mock(PlatformTransactionManager.class));

    private MarketCycleService service(Instant now) {
        return new MarketCycleService(client, marketDataRepository, alertRepository, integrationLogService,
                transactionTemplate, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("SCRUM-153 CA-1: a successful cycle stores the market data and one linked alert")
    void storesWindowAndAlert() {
        when(client.fetchChart()).thenReturn(Fixtures.chart("chart-success.json"));
        when(marketDataRepository.save(any())).thenAnswer(invocation -> {
            MarketData data = invocation.getArgument(0);
            ReflectionTestUtils.setField(data, "id", 42);
            return data;
        });

        MarketCycleService.Outcome outcome = service(Fixtures.ny("11:06:00")).runScheduled();

        assertThat(outcome).isEqualTo(MarketCycleService.Outcome.STORED);
        ArgumentCaptor<MarketData> data = ArgumentCaptor.forClass(MarketData.class);
        verify(marketDataRepository).save(data.capture());
        assertThat(data.getValue().getIndexValue()).isEqualByComparingTo("25131.25");
        assertThat(data.getValue().getChangePercent()).isEqualByComparingTo("0.5250");
        assertThat(data.getValue().getMarketCondition()).isEqualTo("ABIERTO");
        assertThat(data.getValue().getWindowEnd()).isEqualTo(LocalDateTime.parse("2026-10-23T15:05:00"));

        ArgumentCaptor<Alert> alert = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alert.capture());
        assertThat(alert.getValue().getMarketDataId()).isEqualTo(42);
        assertThat(alert.getValue().getAppliedRule()).isEqualTo(RecommendationEngine.RULE);
        assertThat(alert.getValue().getJustification()).startsWith("El RSI");
        assertThat(alert.getValue().getEmittedAt()).isEqualTo(LocalDateTime.parse("2026-10-23T15:05:00"));
        verifyNoInteractions(integrationLogService);
    }

    @Test
    @DisplayName("SCRUM-153 CA-5: a window that already exists is never stored twice")
    void idempotent() {
        when(client.fetchChart()).thenReturn(Fixtures.chart("chart-success.json"));
        when(marketDataRepository.existsByWindowEnd(LocalDateTime.parse("2026-10-23T15:05:00"))).thenReturn(true);

        assertThat(service(Fixtures.ny("11:06:00")).runScheduled())
                .isEqualTo(MarketCycleService.Outcome.ALREADY_PROCESSED);
        verify(marketDataRepository, never()).save(any());
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("SCRUM-153 CA-3: a source failure is logged and creates no alert")
    void sourceFailure() {
        when(client.fetchChart()).thenThrow(MarketDataException.error("Yahoo Finance respondió HTTP 429."));

        MarketCycleService service = service(Fixtures.ny("11:06:00"));
        assertThat(service.runScheduled()).isEqualTo(MarketCycleService.Outcome.FAILED);
        verify(integrationLogService).recordFailure(IntegrationLogService.STATUS_ERROR,
                "Yahoo Finance respondió HTTP 429.");
        verify(alertRepository, never()).save(any());
        assertThat(service.lastFailure().status()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("SCRUM-67 CA-6: insufficient closes are logged as DATOS_INSUFICIENTES")
    void insufficientData() {
        when(client.fetchChart()).thenReturn(Fixtures.chart("chart-few-closes.json"));

        assertThat(service(Fixtures.ny("10:16:00")).runScheduled()).isEqualTo(MarketCycleService.Outcome.FAILED);
        verify(integrationLogService).recordFailure(eq(IntegrationLogService.STATUS_INSUFFICIENT_DATA), anyString());
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("SCRUM-67 CA-8: no cycle runs before the open or after the close")
    void outsideHours() {
        assertThat(service(Fixtures.ny("08:00:00")).runScheduled())
                .isEqualTo(MarketCycleService.Outcome.OUTSIDE_HOURS);
        assertThat(service(Fixtures.ny("17:00:00")).refreshIfStale())
                .isEqualTo(MarketCycleService.Outcome.OUTSIDE_HOURS);
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("On-demand refresh does nothing when the last window is recent")
    void refreshNotNeeded() {
        MarketData recent = new MarketData(null, null, null, "ABIERTO", LocalDateTime.parse("2026-10-23T15:05:00"));
        when(marketDataRepository.findFirstByOrderByWindowEndDescIdDesc()).thenReturn(Optional.of(recent));

        assertThat(service(Fixtures.ny("11:08:00")).refreshIfStale())
                .isEqualTo(MarketCycleService.Outcome.ALREADY_PROCESSED);
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("On-demand refresh runs the cycle when the last window is older than 5 minutes")
    void refreshRuns() {
        MarketData old = new MarketData(null, null, null, "ABIERTO", LocalDateTime.parse("2026-10-23T14:30:00"));
        when(marketDataRepository.findFirstByOrderByWindowEndDescIdDesc()).thenReturn(Optional.of(old));
        when(client.fetchChart()).thenReturn(Fixtures.chart("chart-success.json"));
        when(marketDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service(Fixtures.ny("11:06:00")).refreshIfStale()).isEqualTo(MarketCycleService.Outcome.STORED);
    }
}
