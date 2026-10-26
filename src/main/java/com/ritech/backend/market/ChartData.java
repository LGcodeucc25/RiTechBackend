package com.ritech.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A parsed Yahoo Finance chart response.
 *
 * @param bars bars in the order received; a bar's close may be null
 */
public record ChartData(List<Bar> bars, BigDecimal regularMarketPrice, BigDecimal previousClose,
        BigDecimal chartPreviousClose, Instant fetchedAt) {

    /** A 5-minute bar; {@code start} is the bar's start time. */
    public record Bar(Instant start, BigDecimal close) {

        public Instant end() {
            return start.plus(MarketHours.BAR);
        }
    }
}
