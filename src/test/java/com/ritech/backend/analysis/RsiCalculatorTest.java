package com.ritech.backend.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RsiCalculatorTest {

    /** Wilder's reference series as published by StockCharts. */
    private static final List<BigDecimal> CLOSES = Stream.of(
            "44.3389", "44.0902", "44.1497", "43.6124", "44.3278", "44.8264", "45.0955", "45.4245", "45.8433",
            "46.0826", "45.8931", "46.0328", "45.6140", "46.2820", "46.2820", "46.0028", "46.0328", "46.4116",
            "46.2222", "45.6439", "46.2122", "46.2521", "45.7137", "46.4515", "45.7835", "45.3548", "44.0288",
            "44.1783", "44.2181", "44.5672", "43.4205", "42.6628", "43.1314").map(BigDecimal::new).toList();

    private static final double[] EXPECTED = {
            70.53, 66.32, 66.55, 69.41, 66.36, 57.97, 62.93, 63.26, 56.06, 62.38,
            54.71, 50.42, 39.99, 41.46, 41.87, 45.46, 37.30, 33.08, 37.77};

    @Test
    @DisplayName("SCRUM-67 CA-5: RSI matches Wilder's reference series within 0.01")
    void referenceSeries() {
        List<BigDecimal> series = RsiCalculator.series(CLOSES);
        assertThat(series).hasSize(EXPECTED.length);
        for (int i = 0; i < EXPECTED.length; i++) {
            assertThat(series.get(i).doubleValue()).as("RSI at close %d", i + 14).isCloseTo(EXPECTED[i], within(0.01));
        }
        assertThat(RsiCalculator.latest(CLOSES)).hasValueSatisfying(
                rsi -> assertThat(rsi.doubleValue()).isCloseTo(37.77, within(0.01)));
    }

    @Test
    @DisplayName("SCRUM-67 CA-6: with fewer than 15 closes there is no RSI")
    void notEnoughCloses() {
        assertThat(RsiCalculator.latest(CLOSES.subList(0, 14))).isEmpty();
        assertThat(RsiCalculator.latest(CLOSES.subList(0, 15))).isPresent();
        assertThat(RsiCalculator.latest(null)).isEmpty();
    }

    @Test
    @DisplayName("RSI is 100 when there are no losses and 50 when prices do not move")
    void edgeCases() {
        List<BigDecimal> rising = Stream.iterate(BigDecimal.ONE, v -> v.add(BigDecimal.ONE)).limit(15).toList();
        assertThat(RsiCalculator.latest(rising)).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("100"));
        List<BigDecimal> flat = Collections.nCopies(20, BigDecimal.TEN);
        assertThat(RsiCalculator.latest(flat)).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("50"));
    }
}
