package com.ritech.backend.market;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.analysis.Volatility;

/**
 * One 5-minute window for the chart.
 *
 * @param time           end of the window, Bogotá time
 * @param recommendation the alert stored for this window, if any
 * @param cycleStatus    {@code PROCESSED} when the cycle stored this window, otherwise {@code NOT_PROCESSED}
 */
public record ChartPoint(OffsetDateTime time, BigDecimal close, BigDecimal rsi, Recommendation recommendation,
        Volatility volatility, String cycleStatus) {

    public static final String PROCESSED = "PROCESSED";
    public static final String NOT_PROCESSED = "NOT_PROCESSED";
}
