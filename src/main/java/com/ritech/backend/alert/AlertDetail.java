package com.ritech.backend.alert;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.market.ChartPoint;

public record AlertDetail(
        Integer id,
        Recommendation type,
        String condition,
        BigDecimal rsi,
        OffsetDateTime emittedAt,
        OffsetDateTime emittedAtNewYork,
        String explanation,
        String volatilityWarning,
        List<Criterion> criteria,
        Traceability traceability,
        List<ChartPoint> dayChart) {

    public record Criterion(String criterion, String observed, boolean met) {
    }

    /**
     * @param period     the RSI period, e.g. "14 ventanas de 5 minutos"
     * @param receivedBy the user who is viewing the alert
     */
    public record Traceability(DataUsed dataUsed, String period, String rule, OffsetDateTime emission,
            String receivedBy) {
    }

    public record DataUsed(Integer marketDataId, BigDecimal indexValue, BigDecimal changePercent, BigDecimal rsi,
            OffsetDateTime windowStart, OffsetDateTime windowEnd) {
    }
}
