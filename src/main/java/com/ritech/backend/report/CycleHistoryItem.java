package com.ritech.backend.report;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.ritech.backend.analysis.Recommendation;

/**
 * One cycle in the manager history.
 *
 * @param cycleStatus {@code RECOMMENDATION}, {@code ERROR} or {@code INSUFFICIENT_DATA}
 * @param reason      the cause when the cycle produced no recommendation
 */
public record CycleHistoryItem(OffsetDateTime time, String cycleStatus, Integer alertId, Recommendation type,
        BigDecimal rsi, BigDecimal indexValue, String condition, String reason) {

    public static final String RECOMMENDATION = "RECOMMENDATION";
    public static final String ERROR = "ERROR";
    public static final String INSUFFICIENT_DATA = "INSUFFICIENT_DATA";
}
