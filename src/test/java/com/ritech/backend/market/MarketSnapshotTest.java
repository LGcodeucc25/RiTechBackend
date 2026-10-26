package com.ritech.backend.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ritech.backend.integration.IntegrationLogService;

class MarketSnapshotTest {

    private final ChartData chart = Fixtures.chart("chart-success.json");

    @Test
    @DisplayName("SCRUM-153 CA-2: only completed bars are used and the emission time is the bar's end")
    void usesCompletedBars() {
        MarketSnapshot snapshot = MarketSnapshot.evaluate(chart, Fixtures.ny("11:06:00"));

        assertThat(snapshot.windowEnd()).isEqualTo(Fixtures.ny("11:05:00"));
        assertThat(snapshot.lastClose()).isEqualByComparingTo("25131.25");
        assertThat(snapshot.previousClose()).isEqualByComparingTo("25000.00");
        assertThat(snapshot.changePercent()).isEqualByComparingTo("0.5250");
        assertThat(snapshot.closeCount()).isEqualTo(21);
        assertThat(snapshot.rsi()).isBetween(java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("SCRUM-153 CA-2: a bar that has not finished is ignored")
    void ignoresPartialBar() {
        MarketSnapshot snapshot = MarketSnapshot.evaluate(chart, Fixtures.ny("11:04:59"));
        assertThat(snapshot.windowEnd()).isEqualTo(Fixtures.ny("11:00:00"));
    }

    @Test
    @DisplayName("SCRUM-153 CA-4: data older than 10 minutes is rejected")
    void staleData() {
        assertThatThrownBy(() -> MarketSnapshot.evaluate(chart, Fixtures.ny("11:20:00")))
                .isInstanceOfSatisfying(MarketDataException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(IntegrationLogService.STATUS_ERROR));
    }

    @Test
    @DisplayName("SCRUM-153 CA-4: a null last close is rejected")
    void nullLastClose() {
        assertThatThrownBy(() -> MarketSnapshot.evaluate(chart, Fixtures.ny("10:26:00")))
                .isInstanceOf(MarketDataException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    @DisplayName("SCRUM-67 CA-6: fewer than 15 closes gives insufficient data")
    void fewCloses() {
        ChartData few = Fixtures.chart("chart-few-closes.json");
        assertThatThrownBy(() -> MarketSnapshot.evaluate(few, Fixtures.ny("10:16:00")))
                .isInstanceOfSatisfying(MarketDataException.class,
                        ex -> assertThat(ex.isInsufficientData()).isTrue());
    }

    @Test
    @DisplayName("The previous close falls back to the chart meta when there is no earlier session in the data")
    void previousCloseFallback() {
        ChartData todayOnly = new ChartData(chart.bars().subList(3, chart.bars().size()), null,
                chart.previousClose(), chart.chartPreviousClose(), chart.fetchedAt());
        MarketSnapshot snapshot = MarketSnapshot.evaluate(todayOnly, Fixtures.ny("11:06:00"));
        assertThat(snapshot.previousClose()).isEqualByComparingTo("24990.0");
    }
}
