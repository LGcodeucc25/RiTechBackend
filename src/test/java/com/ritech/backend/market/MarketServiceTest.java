package com.ritech.backend.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.ritech.backend.alert.Alert;
import com.ritech.backend.alert.AlertRepository;
import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.common.ApiException;

class MarketServiceTest {

    private final MarketCycleService cycleService = mock(MarketCycleService.class);
    private final MarketDataProvider provider = mock(MarketDataProvider.class);
    private final MarketDataRepository marketDataRepository = mock(MarketDataRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);

    private MarketService service(Instant now) {
        return new MarketService(cycleService, provider, marketDataRepository, alertRepository,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private void liveChart() {
        when(provider.liveChart()).thenReturn(
                new MarketDataProvider.LiveChart(Optional.of(Fixtures.chart("chart-success.json")), null));
    }

    @Test
    @DisplayName("SCRUM-153 CA-6: after the close the summary reports CLOSED with the last close and no error")
    void closedMarket() {
        liveChart();

        MarketSummaryResponse summary = service(Fixtures.ny("17:00:00")).summary();

        assertThat(summary.marketStatus()).isEqualTo(MarketStatus.CLOSED);
        assertThat(summary.dataStatus()).isEqualTo(DataStatus.OK);
        assertThat(summary.dataStatusMessage()).isEqualTo(MarketService.MSG_OK_CLOSED);
        assertThat(summary.indexValue()).isEqualByComparingTo("25131.25");
        assertThat(summary.previousClose()).isEqualByComparingTo("25000.00");
        assertThat(summary.changePoints()).isEqualByComparingTo("131.25");
        assertThat(summary.changePercent()).isEqualByComparingTo("0.53");
        assertThat(summary.nextCycleAt().toString()).isEqualTo("2026-10-26T08:35-05:00");
        assertThat(summary.currentRecommendation()).isNull();
        assertThat(summary.rsi()).isNotNull();
    }

    @Test
    @DisplayName("SCRUM-67 CA-8: before 9:30 the summary reports PRE_MARKET")
    void preMarket() {
        liveChart();
        assertThat(service(Fixtures.ny("08:00:00")).summary().marketStatus()).isEqualTo(MarketStatus.PRE_MARKET);
    }

    @Test
    @DisplayName("The summary shows the latest stored recommendation")
    void currentRecommendation() {
        liveChart();
        Alert alert = new Alert(1, Recommendation.SELL, "sobrecompra", new BigDecimal("72.06"), "Explicación",
                LocalDateTime.parse("2026-10-23T15:05:00"), "RSI14 Wilder 30/70");
        when(alertRepository.findFirstByOrderByEmittedAtDescIdDesc()).thenReturn(Optional.of(alert));

        MarketSummaryResponse summary = service(Fixtures.ny("17:00:00")).summary();

        assertThat(summary.currentRecommendation().type()).isEqualTo(Recommendation.SELL);
        assertThat(summary.rsi()).isEqualByComparingTo("72.06");
        assertThat(summary.condition()).isEqualTo("sobrecompra");
    }

    @Test
    @DisplayName("SCRUM-153 CA-4: during market hours old data is reported as STALE")
    void staleDuringMarketHours() {
        liveChart();
        MarketData old = new MarketData(new BigDecimal("25000"), new BigDecimal("0.1"), null, "ABIERTO",
                LocalDateTime.parse("2026-10-23T14:00:00"));
        when(marketDataRepository.findFirstByOrderByWindowEndDescIdDesc()).thenReturn(Optional.of(old));

        MarketSummaryResponse summary = service(Fixtures.ny("11:06:00")).summary();

        assertThat(summary.dataStatus()).isEqualTo(DataStatus.STALE);
        assertThat(summary.marketStatus()).isEqualTo(MarketStatus.OPEN);
    }

    @Test
    @DisplayName("A source error without any stored data returns 503")
    void sourceUnavailable() {
        when(provider.liveChart()).thenReturn(new MarketDataProvider.LiveChart(Optional.empty(),
                MarketDataException.error("Yahoo Finance respondió HTTP 503.")));

        assertThatThrownBy(() -> service(Fixtures.ny("17:00:00")).summary())
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("A source error with stored data falls back to it and reports SOURCE_ERROR")
    void sourceErrorFallback() {
        when(provider.liveChart()).thenReturn(new MarketDataProvider.LiveChart(Optional.empty(),
                MarketDataException.error("Yahoo Finance respondió HTTP 503.")));
        MarketData stored = new MarketData(new BigDecimal("25131.25"), new BigDecimal("0.5250"),
                new BigDecimal("61.2"), "ABIERTO", LocalDateTime.parse("2026-10-23T15:05:00"));
        when(marketDataRepository.findFirstByOrderByWindowEndDescIdDesc()).thenReturn(Optional.of(stored));

        MarketSummaryResponse summary = service(Fixtures.ny("17:00:00")).summary();

        assertThat(summary.dataStatus()).isEqualTo(DataStatus.SOURCE_ERROR);
        assertThat(summary.indexValue()).isEqualByComparingTo("25131.25");
        assertThat(summary.previousClose()).isEqualByComparingTo("25000.00");
    }

    @Test
    @DisplayName("The 1d history has the last session's completed bars with RSI after 14 windows and alerts overlaid")
    void historyOneDay() {
        liveChart();
        Alert alert = new Alert(1, Recommendation.SELL, "sobrecompra", new BigDecimal("72.06"), "Explicación",
                LocalDateTime.parse("2026-10-23T15:05:00"), "RSI14 Wilder 30/70");
        when(alertRepository.findByEmittedAtBetweenOrderByEmittedAtAsc(any(), any())).thenReturn(List.of(alert));

        List<ChartPoint> points = service(Fixtures.ny("17:00:00")).history("1d");

        assertThat(points).hasSize(18);
        assertThat(points.getFirst().time().toString()).isEqualTo("2026-10-23T08:35-05:00");
        assertThat(points.getFirst().volatility()).isEqualTo(com.ritech.backend.analysis.Volatility.OPENING);
        assertThat(points.getLast().recommendation()).isEqualTo(Recommendation.SELL);
        assertThat(points.getLast().rsi()).isNotNull();
        assertThat(service(Fixtures.ny("17:00:00")).history("5d")).hasSize(21);
    }

    @Test
    @DisplayName("An invalid history range returns 400")
    void invalidRange() {
        assertThatThrownBy(() -> service(Fixtures.ny("17:00:00")).history("2w")).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("Points for a day outside the live chart come from stored data")
    void pointsForStoredDay() {
        liveChart();
        when(marketDataRepository.findByWindowEndBetweenOrderByWindowEndAsc(any(), any())).thenReturn(List.of());
        assertThat(service(Fixtures.ny("17:00:00")).pointsForDay(LocalDate.parse("2026-10-01"))).isEmpty();
    }
}
