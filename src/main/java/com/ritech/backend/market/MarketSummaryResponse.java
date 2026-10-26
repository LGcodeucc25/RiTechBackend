package com.ritech.backend.market;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.ritech.backend.alert.AlertSummary;

public record MarketSummaryResponse(
        BigDecimal indexValue,
        BigDecimal changePercent,
        BigDecimal changePoints,
        BigDecimal previousClose,
        MarketStatus marketStatus,
        OffsetDateTime lastUpdate,
        OffsetDateTime nextCycleAt,
        BigDecimal rsi,
        String condition,
        AlertSummary currentRecommendation,
        DataStatus dataStatus,
        String dataStatusMessage) {
}
