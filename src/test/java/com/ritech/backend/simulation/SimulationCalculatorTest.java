package com.ritech.backend.simulation;

import static com.ritech.backend.simulation.SimulationCalculator.money;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationCalculatorTest {

    private static final BigDecimal CAPITAL = new BigDecimal("1000000");

    private static SimulationCalculator.Result run(String variation) {
        return SimulationCalculator.calculate(CAPITAL, new BigDecimal(variation));
    }

    @Test
    @DisplayName("SCRUM-63 CA-1: +2% on 1,000,000 gives +4%, +40,000.00 and 1,040,000.00")
    void positiveVariation() {
        SimulationCalculator.Result result = run("2");
        assertThat(money(result.resultPercent())).isEqualByComparingTo("4.00");
        assertThat(money(result.resultAmount())).isEqualByComparingTo("40000.00");
        assertThat(money(result.finalValue())).isEqualByComparingTo("1040000.00");
        assertThat(result.capped()).isFalse();
    }

    @Test
    @DisplayName("SCRUM-64 CA-1: -2% on 1,000,000 gives -4%, -40,000.00 and 960,000.00")
    void negativeVariation() {
        SimulationCalculator.Result result = run("-2");
        assertThat(money(result.resultPercent())).isEqualByComparingTo("-4.00");
        assertThat(money(result.resultAmount())).isEqualByComparingTo("-40000.00");
        assertThat(money(result.finalValue())).isEqualByComparingTo("960000.00");
    }

    @Test
    @DisplayName("SCRUM-65 CA-1: 0% keeps the capital unchanged")
    void zeroVariation() {
        SimulationCalculator.Result result = run("0");
        assertThat(money(result.resultAmount())).isEqualByComparingTo("0.00");
        assertThat(money(result.finalValue())).isEqualByComparingTo("1000000.00");
    }

    @Test
    @DisplayName("SCRUM-63 CA-2: +1.5% gives +3%")
    void decimalVariation() {
        assertThat(money(run("1.5").resultPercent())).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("SCRUM-64 CA-4: a drop over 50% caps the loss at 100%")
    void cappedLoss() {
        SimulationCalculator.Result result = run("-60");
        assertThat(money(result.resultPercent())).isEqualByComparingTo("-100.00");
        assertThat(money(result.resultAmount())).isEqualByComparingTo("-1000000.00");
        assertThat(money(result.finalValue())).isEqualByComparingTo("0.00");
        assertThat(result.capped()).isTrue();
    }

    @Test
    @DisplayName("SCRUM-169: the maximum loss is always the capital")
    void maxLoss() {
        assertThat(run("10").maxLoss()).isEqualByComparingTo(CAPITAL);
        assertThat(run("-10").maxLoss()).isEqualByComparingTo(CAPITAL);
    }

    @Test
    @DisplayName("Precision is kept internally and rounded HALF_UP only at the end")
    void rounding() {
        SimulationCalculator.Result result = SimulationCalculator.calculate(new BigDecimal("0.05"),
                new BigDecimal("12.5"));
        assertThat(result.resultAmount()).isEqualByComparingTo("0.0125");
        assertThat(money(result.resultAmount())).isEqualByComparingTo("0.01");
        assertThat(money(new BigDecimal("0.005"))).isEqualByComparingTo("0.01");
    }
}
