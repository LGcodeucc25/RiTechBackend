package com.ritech.backend.analysis;

import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.ritech.backend.common.DateTimes;

/**
 * High-volatility windows: the first 15 minutes after the 9:30 open and the last 15 minutes before the 16:00 close
 * (New York time), judged by the end of the 5-minute window.
 */
public enum Volatility {
    NONE,
    OPENING,
    CLOSING;

    static final LocalTime OPEN = LocalTime.of(9, 30);
    static final LocalTime OPENING_LIMIT = LocalTime.of(9, 45);
    static final LocalTime CLOSING_LIMIT = LocalTime.of(15, 45);
    static final LocalTime CLOSE = LocalTime.of(16, 0);

    public static Volatility of(ZonedDateTime windowEnd) {
        LocalTime time = windowEnd.withZoneSameInstant(DateTimes.NEW_YORK).toLocalTime();
        if (time.isAfter(OPEN) && !time.isAfter(OPENING_LIMIT)) {
            return OPENING;
        }
        if (time.isAfter(CLOSING_LIMIT) && !time.isAfter(CLOSE)) {
            return CLOSING;
        }
        return NONE;
    }

    public boolean isHigh() {
        return this != NONE;
    }
}
