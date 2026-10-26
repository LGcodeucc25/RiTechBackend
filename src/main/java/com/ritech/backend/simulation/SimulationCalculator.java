package com.ritech.backend.simulation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Symmetric x2 rule: the investment moves twice the expected NASDAQ 100 change, and a loss never exceeds the capital.
 */
public final class SimulationCalculator {

    public static final BigDecimal LEVERAGE = BigDecimal.valueOf(2);
    private static final BigDecimal MINUS_ONE_HUNDRED = BigDecimal.valueOf(-100);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private SimulationCalculator() {
    }

    /** Full-precision result; round with {@link #money(BigDecimal)} only when storing or returning. */
    public static Result calculate(BigDecimal capital, BigDecimal variation) {
        BigDecimal rawPercent = variation.multiply(LEVERAGE);
        BigDecimal resultPercent = rawPercent.max(MINUS_ONE_HUNDRED);
        BigDecimal resultAmount = capital.multiply(resultPercent).divide(ONE_HUNDRED, MathContext.DECIMAL128);
        BigDecimal finalValue = capital.add(resultAmount).max(BigDecimal.ZERO);
        boolean capped = rawPercent.compareTo(MINUS_ONE_HUNDRED) < 0;
        return new Result(rawPercent, resultPercent, resultAmount, finalValue, capital, capped);
    }

    public static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    public record Result(BigDecimal rawPercent, BigDecimal resultPercent, BigDecimal resultAmount,
            BigDecimal finalValue, BigDecimal maxLoss, boolean capped) {
    }
}
