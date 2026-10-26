package com.ritech.backend.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Timestamps are stored as UTC LocalDateTime values and exposed with the Bogotá offset.
 */
public final class DateTimes {

    public static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private DateTimes() {
    }

    public static LocalDateTime nowUtc(Clock clock) {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC)).truncatedTo(ChronoUnit.SECONDS);
    }

    public static OffsetDateTime toBogota(LocalDateTime utc) {
        return utc == null ? null : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(BOGOTA).toOffsetDateTime();
    }

    public static OffsetDateTime toBogota(Instant instant) {
        return instant == null ? null : instant.atZone(BOGOTA).toOffsetDateTime();
    }

    public static OffsetDateTime toNewYork(LocalDateTime utc) {
        return utc == null ? null : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(NEW_YORK).toOffsetDateTime();
    }

    public static LocalDateTime toUtc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static Instant toInstant(LocalDateTime utc) {
        return utc.toInstant(ZoneOffset.UTC);
    }

    public static LocalDateTime toUtc(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDateTime toUtc(OffsetDateTime dateTime) {
        return dateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
