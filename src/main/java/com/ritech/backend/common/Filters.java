package com.ritech.backend.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;

import com.ritech.backend.analysis.Recommendation;

/**
 * Parsing of the query filters shared by the alert and report endpoints.
 */
public final class Filters {

    private static final LocalDateTime MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime MAX = LocalDateTime.of(2100, 1, 1, 0, 0);

    private Filters() {
    }

    /** Comma-separated BUY, SELL, HOLD; blank gives the default. */
    public static List<Recommendation> types(String raw, List<Recommendation> defaults) {
        if (raw == null || raw.isBlank()) {
            return defaults;
        }
        List<Recommendation> types = new ArrayList<>();
        for (String part : Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()) {
            try {
                types.add(Recommendation.valueOf(part.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TYPE",
                        "El tipo de recomendación debe ser BUY, SELL o HOLD.");
            }
        }
        return types.isEmpty() ? defaults : types;
    }

    /** Start of the given Bogotá day in UTC, or a distant past. */
    public static LocalDateTime fromDay(LocalDate from) {
        return from == null ? MIN : DateTimes.toUtc(from.atStartOfDay(DateTimes.BOGOTA));
    }

    /** End (exclusive) of the given Bogotá day in UTC, or a distant future. */
    public static LocalDateTime toDay(LocalDate to) {
        return to == null ? MAX : DateTimes.toUtc(to.plusDays(1).atStartOfDay(DateTimes.BOGOTA));
    }

    public static void checkRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "La fecha inicial no puede ser posterior a la fecha final.");
        }
    }
}
