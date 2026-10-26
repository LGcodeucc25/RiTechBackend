package com.ritech.backend.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RSI(14) with Wilder's smoothing.
 */
public final class RsiCalculator {

    public static final int PERIOD = 14;
    public static final int MIN_CLOSES = PERIOD + 1;

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal PERIOD_BD = BigDecimal.valueOf(PERIOD);
    private static final BigDecimal PERIOD_MINUS_ONE = BigDecimal.valueOf(PERIOD - 1);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal FIFTY = BigDecimal.valueOf(50);

    private RsiCalculator() {
    }

    /** RSI of the last close, or empty with fewer than 15 closes. */
    public static Optional<BigDecimal> latest(List<BigDecimal> closes) {
        List<BigDecimal> series = series(closes);
        return series.isEmpty() ? Optional.empty() : Optional.of(series.getLast());
    }

    /**
     * RSI for every close from index 14 onward; element i of the result belongs to close i + 14.
     */
    public static List<BigDecimal> series(List<BigDecimal> closes) {
        List<BigDecimal> result = new ArrayList<>();
        if (closes == null || closes.size() < MIN_CLOSES) {
            return result;
        }
        BigDecimal gainSum = BigDecimal.ZERO;
        BigDecimal lossSum = BigDecimal.ZERO;
        for (int i = 1; i <= PERIOD; i++) {
            BigDecimal change = closes.get(i).subtract(closes.get(i - 1));
            gainSum = gainSum.add(change.max(BigDecimal.ZERO));
            lossSum = lossSum.add(change.min(BigDecimal.ZERO).negate());
        }
        BigDecimal avgGain = gainSum.divide(PERIOD_BD, MC);
        BigDecimal avgLoss = lossSum.divide(PERIOD_BD, MC);
        result.add(rsi(avgGain, avgLoss));

        for (int i = PERIOD + 1; i < closes.size(); i++) {
            BigDecimal change = closes.get(i).subtract(closes.get(i - 1));
            BigDecimal gain = change.max(BigDecimal.ZERO);
            BigDecimal loss = change.min(BigDecimal.ZERO).negate();
            avgGain = avgGain.multiply(PERIOD_MINUS_ONE).add(gain).divide(PERIOD_BD, MC);
            avgLoss = avgLoss.multiply(PERIOD_MINUS_ONE).add(loss).divide(PERIOD_BD, MC);
            result.add(rsi(avgGain, avgLoss));
        }
        return result;
    }

    static BigDecimal rsi(BigDecimal avgGain, BigDecimal avgLoss) {
        if (avgLoss.signum() == 0) {
            return avgGain.signum() == 0 ? FIFTY : HUNDRED;
        }
        BigDecimal rs = avgGain.divide(avgLoss, MC);
        return HUNDRED.subtract(HUNDRED.divide(BigDecimal.ONE.add(rs), MC));
    }
}
