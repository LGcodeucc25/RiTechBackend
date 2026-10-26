package com.ritech.backend.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ritech.backend.analysis.Recommendation;

class ReportCalculatorTest {

    private static final Instant T0 = Instant.parse("2026-10-23T14:00:00Z");

    private static ReportCalculator.Signal signal(int minutes, Recommendation type, String close) {
        return new ReportCalculator.Signal(T0.plusSeconds(60L * minutes), type, new BigDecimal(close));
    }

    @Test
    @DisplayName("RF-031: buy on the first BUY, sell on the next SELL, each trade returns 2x the index change")
    void closedTrades() {
        List<ReportCalculator.Signal> signals = List.of(
                signal(0, Recommendation.HOLD, "100"),
                signal(5, Recommendation.BUY, "100"),
                signal(10, Recommendation.BUY, "99"),
                signal(15, Recommendation.SELL, "101"),
                signal(20, Recommendation.SELL, "102"),
                signal(25, Recommendation.BUY, "100"),
                signal(30, Recommendation.SELL, "99"));

        ReportCalculator.Performance result = ReportCalculator.performance(signals, new BigDecimal("100"),
                T0.plusSeconds(1800), new BigDecimal("99"));

        assertThat(result.trades()).hasSize(2);
        assertThat(result.trades().get(0).returnPercent()).isEqualByComparingTo("2.00");
        assertThat(result.trades().get(1).returnPercent()).isEqualByComparingTo("-2.00");
        // (1.02 * 0.98 - 1) * 100 = -0.04
        assertThat(result.strategyReturnPercent()).isEqualByComparingTo("-0.04");
        assertThat(result.buyAndHoldReturnPercent()).isEqualByComparingTo("-2.00");
    }

    @Test
    @DisplayName("RF-031: an open position is valued at the last close")
    void openPosition() {
        ReportCalculator.Performance result = ReportCalculator.performance(
                List.of(signal(5, Recommendation.BUY, "200")), new BigDecimal("190"), T0.plusSeconds(600),
                new BigDecimal("210"));

        assertThat(result.trades()).singleElement().satisfies(trade -> {
            assertThat(trade.open()).isTrue();
            assertThat(trade.returnPercent()).isEqualByComparingTo("10.00");
        });
        assertThat(result.strategyReturnPercent()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("RF-031: a trade never loses more than 100%")
    void cappedLoss() {
        assertThat(ReportCalculator.leveragedReturn(new BigDecimal("100"), new BigDecimal("40")))
                .isEqualByComparingTo("-100");
    }

    @Test
    @DisplayName("Without trades the strategy return is 0")
    void noTrades() {
        ReportCalculator.Performance result = ReportCalculator.performance(List.of(), new BigDecimal("100"), T0,
                new BigDecimal("101"));
        assertThat(result.strategyReturnPercent()).isEqualByComparingTo("0.00");
        assertThat(result.buyAndHoldReturnPercent()).isEqualByComparingTo("2.00");
    }

    @Test
    @DisplayName("RF-031: expected cycles count the windows that have ended, 78 per full session")
    void expectedCycles() {
        LocalDate day = LocalDate.parse("2026-10-23");
        assertThat(ReportCalculator.expectedCycles(List.of(day), Instant.parse("2026-10-23T21:00:00Z"))).isEqualTo(78);
        // 10:00 New York (EDT): windows ending 9:35 ... 10:00
        assertThat(ReportCalculator.expectedCycles(List.of(day), Instant.parse("2026-10-23T14:00:00Z"))).isEqualTo(6);
        assertThat(ReportCalculator.expectedCycles(List.of(day), Instant.parse("2026-10-23T12:00:00Z"))).isZero();
    }

    @Test
    @DisplayName("Trading days skip weekends and end on the latest weekday")
    void tradingDays() {
        Instant sunday = Instant.parse("2026-10-25T15:00:00Z");
        assertThat(ReportCalculator.tradingDays(sunday, 1)).containsExactly(LocalDate.parse("2026-10-23"));
        assertThat(ReportCalculator.tradingDays(sunday, 5)).containsExactly(
                LocalDate.parse("2026-10-19"), LocalDate.parse("2026-10-20"), LocalDate.parse("2026-10-21"),
                LocalDate.parse("2026-10-22"), LocalDate.parse("2026-10-23"));
    }

    @Test
    @DisplayName("Percentages are rounded to 2 decimals and undefined without a base")
    void percent() {
        assertThat(ReportCalculator.percent(1, 3)).isEqualByComparingTo("33.33");
        assertThat(ReportCalculator.percent(0, 0)).isNull();
    }
}
