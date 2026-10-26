package com.ritech.backend.market;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The latest index value in a chart, regardless of its age, for display.
 */
public record MarketQuote(BigDecimal lastClose, BigDecimal previousClose, BigDecimal changePoints,
        BigDecimal changePercent, Instant time) {

    public static Optional<MarketQuote> from(ChartData chart) {
        List<ChartData.Bar> bars = sessionBars(chart);
        if (bars.isEmpty()) {
            return Optional.empty();
        }
        ChartData.Bar last = bars.getLast();
        BigDecimal previous;
        try {
            previous = MarketSnapshot.previousClose(bars, last, chart);
        } catch (MarketDataException ex) {
            return Optional.empty();
        }
        BigDecimal points = last.close().subtract(previous);
        BigDecimal percent = points.multiply(BigDecimal.valueOf(100)).divide(previous, MathContext.DECIMAL64);
        return Optional.of(new MarketQuote(last.close(), previous, points.setScale(2, RoundingMode.HALF_UP),
                percent.setScale(2, RoundingMode.HALF_UP), last.end()));
    }

    /** Regular-session bars with a close, oldest first. */
    public static List<ChartData.Bar> sessionBars(ChartData chart) {
        return chart.bars().stream()
                .filter(bar -> bar.close() != null && MarketHours.isSessionBar(bar.start()))
                .filter(bar -> !bar.end().isAfter(chart.fetchedAt()))
                .sorted(Comparator.comparing(ChartData.Bar::start))
                .toList();
    }
}
