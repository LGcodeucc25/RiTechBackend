package com.ritech.backend.report;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.common.DateTimes;
import com.ritech.backend.market.MarketHours;

/**
 * Pure calculations behind the manager statistics.
 */
public final class ReportCalculator {

    public static final int WINDOWS_PER_SESSION = 78;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MINUS_HUNDRED = BigDecimal.valueOf(-100);
    private static final BigDecimal LEVERAGE = BigDecimal.valueOf(2);

    private ReportCalculator() {
    }

    /** A recommendation with the index close of its window. */
    public record Signal(Instant time, Recommendation type, BigDecimal close) {
    }

    public record Trade(Instant buyTime, BigDecimal buyPrice, Instant sellTime, BigDecimal sellPrice,
            BigDecimal returnPercent, boolean open) {
    }

    public record Performance(List<Trade> trades, BigDecimal strategyReturnPercent,
            BigDecimal buyAndHoldReturnPercent) {
    }

    /** Trading days (Monday to Friday) of a 1d or 5d period ending on the latest trading day up to {@code now}. */
    public static List<LocalDate> tradingDays(Instant now, int count) {
        LocalDate day = now.atZone(DateTimes.NEW_YORK).toLocalDate();
        List<LocalDate> days = new ArrayList<>();
        while (days.size() < count) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.addFirst(day);
            }
            day = day.minusDays(1);
        }
        return days;
    }

    /** 5-minute windows of the given trading days that had ended by {@code now}. */
    public static int expectedCycles(List<LocalDate> days, Instant now) {
        int total = 0;
        for (LocalDate day : days) {
            ZonedDateTime open = day.atTime(MarketHours.OPEN).atZone(DateTimes.NEW_YORK);
            for (int i = 1; i <= WINDOWS_PER_SESSION; i++) {
                if (!open.plusMinutes(5L * i).toInstant().isAfter(now)) {
                    total++;
                }
            }
        }
        return total;
    }

    public static BigDecimal percent(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return BigDecimal.valueOf(part).multiply(HUNDRED).divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    /** Index change between two closes, times two, capped at -100%. */
    public static BigDecimal leveragedReturn(BigDecimal from, BigDecimal to) {
        BigDecimal change = to.subtract(from).multiply(HUNDRED).divide(from, MathContext.DECIMAL64);
        return change.multiply(LEVERAGE).max(MINUS_HUNDRED);
    }

    /**
     * Buys on the first BUY and sells on the next SELL, repeatedly; an open position is valued at the last close.
     * Trade returns are compounded. The benchmark holds the x2 product from the first to the last close.
     */
    public static Performance performance(List<Signal> signals, BigDecimal firstClose, Instant lastTime,
            BigDecimal lastClose) {
        List<Trade> trades = new ArrayList<>();
        Signal entry = null;
        for (Signal signal : signals) {
            if (entry == null && signal.type() == Recommendation.BUY) {
                entry = signal;
            } else if (entry != null && signal.type() == Recommendation.SELL) {
                trades.add(new Trade(entry.time(), entry.close(), signal.time(), signal.close(),
                        round(leveragedReturn(entry.close(), signal.close())), false));
                entry = null;
            }
        }
        if (entry != null && lastClose != null) {
            trades.add(new Trade(entry.time(), entry.close(), lastTime, lastClose,
                    round(leveragedReturn(entry.close(), lastClose)), true));
        }
        BigDecimal growth = BigDecimal.ONE;
        for (Trade trade : trades) {
            growth = growth.multiply(BigDecimal.ONE.add(trade.returnPercent().divide(HUNDRED, MathContext.DECIMAL64)));
        }
        BigDecimal strategy = round(growth.subtract(BigDecimal.ONE).multiply(HUNDRED));
        BigDecimal benchmark = firstClose == null || lastClose == null ? null
                : round(leveragedReturn(firstClose, lastClose));
        return new Performance(trades, strategy, benchmark);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
