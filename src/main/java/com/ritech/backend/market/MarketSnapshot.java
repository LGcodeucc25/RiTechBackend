package com.ritech.backend.market;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.ritech.backend.analysis.RsiCalculator;
import com.ritech.backend.common.DateTimes;

/**
 * The latest completed 5-minute window derived from a chart, validated for completeness and freshness.
 *
 * @param windowEnd     end of the last completed bar (the emission time)
 * @param changePercent change of the last close against the previous session close, in percent
 * @param rsi           full-precision RSI(14) of the completed closes
 */
public record MarketSnapshot(Instant windowEnd, BigDecimal lastClose, BigDecimal previousClose,
        BigDecimal changePercent, BigDecimal rsi, int closeCount) {

    public static final Duration MAX_AGE = Duration.ofMinutes(10);

    public static MarketSnapshot evaluate(ChartData chart, Instant now) {
        List<ChartData.Bar> completed = chart.bars().stream()
                .filter(bar -> MarketHours.isSessionBar(bar.start()))
                .filter(bar -> !bar.end().isAfter(now))
                .sorted(Comparator.comparing(ChartData.Bar::start))
                .toList();
        if (completed.isEmpty()) {
            throw MarketDataException.insufficient("No hay barras de 5 minutos completas.");
        }
        ChartData.Bar last = completed.getLast();
        if (last.close() == null) {
            throw MarketDataException.error("El último cierre de 5 minutos llegó vacío.");
        }
        if (last.end().isBefore(now.minus(MAX_AGE))) {
            throw MarketDataException.stale("Los datos tienen más de 10 minutos de antigüedad.");
        }
        List<BigDecimal> closes = completed.stream().map(ChartData.Bar::close).filter(Objects::nonNull).toList();
        if (closes.size() < RsiCalculator.MIN_CLOSES) {
            throw MarketDataException.insufficient("Solo hay " + closes.size()
                    + " cierres válidos; se necesitan al menos 15 para el RSI.");
        }
        BigDecimal rsi = RsiCalculator.latest(closes).orElseThrow();
        BigDecimal previousClose = previousClose(completed, last, chart);
        BigDecimal changePercent = last.close().subtract(previousClose)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousClose, MathContext.DECIMAL64)
                .setScale(4, RoundingMode.HALF_UP);
        return new MarketSnapshot(last.end(), last.close(), previousClose, changePercent, rsi, closes.size());
    }

    /** The last close of the previous session in the data, falling back to the values in the chart's meta. */
    static BigDecimal previousClose(List<ChartData.Bar> completed, ChartData.Bar last, ChartData chart) {
        LocalDate lastDay = last.start().atZone(DateTimes.NEW_YORK).toLocalDate();
        BigDecimal fromBars = completed.stream()
                .filter(bar -> bar.close() != null)
                .filter(bar -> bar.start().atZone(DateTimes.NEW_YORK).toLocalDate().isBefore(lastDay))
                .reduce((first, second) -> second)
                .map(ChartData.Bar::close)
                .orElse(null);
        BigDecimal value = fromBars != null ? fromBars
                : chart.previousClose() != null ? chart.previousClose() : chart.chartPreviousClose();
        if (value == null || value.signum() <= 0) {
            throw MarketDataException.insufficient("No se encontró el cierre anterior del índice.");
        }
        return value;
    }
}
