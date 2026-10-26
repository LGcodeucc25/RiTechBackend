package com.ritech.backend.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

/**
 * Classifies an RSI value with the 30/70 rule and builds its plain-language explanation.
 */
public final class RecommendationEngine {

    public static final String RULE = "RSI14 Wilder 30/70";
    public static final BigDecimal OVERSOLD = new BigDecimal("30.00");
    public static final BigDecimal OVERBOUGHT = new BigDecimal("70.00");

    public static final String OVERSOLD_CONDITION = "sobreventa";
    public static final String OVERBOUGHT_CONDITION = "sobrecompra";
    public static final String NEUTRAL_CONDITION = "condición neutra";
    public static final String HIGH_VOLATILITY_SUFFIX = " (alta volatilidad)";

    public static final String OPENING_WARNING = "Se emitió en los primeros 15 minutos de negociación, una franja de "
            + "alta volatilidad: el precio puede cambiar con rapidez y la señal es menos confiable.";
    public static final String CLOSING_WARNING = "Se emitió en los últimos 15 minutos antes del cierre, una franja de "
            + "alta volatilidad: el precio puede cambiar con rapidez y la señal es menos confiable.";

    private RecommendationEngine() {
    }

    public static Analysis analyze(BigDecimal rawRsi, ZonedDateTime windowEnd) {
        BigDecimal rsi = round(rawRsi);
        Recommendation recommendation = classify(rsi);
        Volatility volatility = Volatility.of(windowEnd);
        String condition = condition(recommendation);
        String storedCondition = volatility.isHigh() ? condition + HIGH_VOLATILITY_SUFFIX : condition;
        return new Analysis(recommendation, rsi, condition, storedCondition, explain(recommendation, rsi, volatility),
                volatility);
    }

    public static BigDecimal round(BigDecimal rsi) {
        return rsi.setScale(2, RoundingMode.HALF_UP);
    }

    /** Expects the RSI already rounded to 2 decimals. */
    public static Recommendation classify(BigDecimal rsi) {
        if (rsi.compareTo(OVERSOLD) <= 0) {
            return Recommendation.BUY;
        }
        if (rsi.compareTo(OVERBOUGHT) >= 0) {
            return Recommendation.SELL;
        }
        return Recommendation.HOLD;
    }

    public static String condition(Recommendation recommendation) {
        return switch (recommendation) {
            case BUY -> OVERSOLD_CONDITION;
            case SELL -> OVERBOUGHT_CONDITION;
            case HOLD -> NEUTRAL_CONDITION;
        };
    }

    public static String explain(Recommendation recommendation, BigDecimal rsi, Volatility volatility) {
        String value = format(rsi);
        String text = switch (recommendation) {
            case SELL -> "El RSI alcanzó " + value + ", "
                    + (rsi.compareTo(OVERBOUGHT) == 0 ? "justo en el límite de 70" : "por encima del límite de 70")
                    + ". El índice se considera sobrecomprado, por lo que se recomienda VENDER.";
            case BUY -> "El RSI bajó a " + value + ", "
                    + (rsi.compareTo(OVERSOLD) == 0 ? "justo en el límite de 30" : "por debajo del límite de 30")
                    + ". El índice se considera sobrevendido, por lo que se recomienda COMPRAR.";
            case HOLD -> "El RSI está en " + value + ", entre los límites de 30 y 70. El índice no muestra "
                    + "sobrecompra ni sobreventa, por lo que se recomienda MANTENER.";
        };
        String warning = warning(volatility);
        return warning == null ? text : text + " " + warning;
    }

    /** The volatility warning, or null outside the high-volatility windows. */
    public static String warning(Volatility volatility) {
        return switch (volatility) {
            case OPENING -> OPENING_WARNING;
            case CLOSING -> CLOSING_WARNING;
            case NONE -> null;
        };
    }

    /** Decimal comma and 2 decimals, e.g. 72,06. */
    public static String format(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
