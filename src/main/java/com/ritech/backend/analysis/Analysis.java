package com.ritech.backend.analysis;

import java.math.BigDecimal;

/**
 * A recommendation together with everything that is stored with it.
 *
 * @param rsi           the RSI rounded to 2 decimals, used for both display and decision
 * @param condition     the plain condition, e.g. "sobreventa"
 * @param storedCondition the condition plus " (alta volatilidad)" when it applies
 * @param explanation   the full Spanish explanation, including the volatility warning
 */
public record Analysis(Recommendation recommendation, BigDecimal rsi, String condition, String storedCondition,
        String explanation, Volatility volatility) {
}
