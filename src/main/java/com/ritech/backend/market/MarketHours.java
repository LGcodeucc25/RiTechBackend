package com.ritech.backend.market;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.ritech.backend.common.DateTimes;

/**
 * Regular NASDAQ session, 9:30 to 16:00 New York time, Monday to Friday. Exchange holidays are not modelled; on those
 * days the source returns old data and the cycle records it as a failure.
 */
public final class MarketHours {

    public static final LocalTime OPEN = LocalTime.of(9, 30);
    public static final LocalTime CLOSE = LocalTime.of(16, 0);
    public static final Duration BAR = Duration.ofMinutes(5);

    private MarketHours() {
    }

    public static MarketStatus status(Instant now) {
        ZonedDateTime ny = now.atZone(DateTimes.NEW_YORK);
        if (!isWeekday(ny.toLocalDate())) {
            return MarketStatus.CLOSED;
        }
        LocalTime time = ny.toLocalTime();
        if (time.isBefore(OPEN)) {
            return MarketStatus.PRE_MARKET;
        }
        return time.isBefore(CLOSE) ? MarketStatus.OPEN : MarketStatus.CLOSED;
    }

    /** A cycle can run once a bar of today's session has completed, up to and including the 16:00 close. */
    public static boolean isCycleTime(Instant now) {
        ZonedDateTime ny = now.atZone(DateTimes.NEW_YORK);
        LocalTime time = ny.toLocalTime();
        return isWeekday(ny.toLocalDate()) && !time.isBefore(OPEN.plus(BAR)) && !time.isAfter(CLOSE);
    }

    /** True if the bar starting at this instant belongs to the regular session grid (9:30 to 15:55, every 5 min). */
    public static boolean isSessionBar(Instant barStart) {
        ZonedDateTime ny = barStart.atZone(DateTimes.NEW_YORK);
        LocalTime time = ny.toLocalTime();
        return isWeekday(ny.toLocalDate()) && !time.isBefore(OPEN) && time.isBefore(CLOSE)
                && time.getMinute() % 5 == 0 && time.getSecond() == 0;
    }

    /** The next time the scheduler will process a window. */
    public static ZonedDateTime nextCycleAt(Instant now) {
        ZonedDateTime ny = now.atZone(DateTimes.NEW_YORK).truncatedTo(ChronoUnit.MINUTES);
        ZonedDateTime candidate = ny.plusMinutes(5 - ny.getMinute() % 5);
        for (int i = 0; i < 8 * 24 * 12; i++) {
            if (isCycleTime(candidate.toInstant())) {
                return candidate;
            }
            candidate = candidate.plusMinutes(5);
        }
        return candidate;
    }

    private static boolean isWeekday(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
