package com.ritech.backend.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record StatisticsResponse(
        String range,
        LocalDate from,
        LocalDate to,
        Counts counts,
        BigDecimal volatilityWarningPercent,
        CyclesWithoutRecommendation cyclesWithoutRecommendation,
        DataAvailability dataAvailability,
        TheoreticalPerformance theoreticalPerformance,
        List<DailySignals> signalsPerDay) {

    public record Counts(long buy, long sell, long hold) {
    }

    public record CyclesWithoutRecommendation(long count, List<FailedCycle> cycles) {
    }

    public record FailedCycle(OffsetDateTime time, String status, String reason) {
    }

    /** @param percent valid cycles divided by expected cycles, or null when no cycle was expected yet */
    public record DataAvailability(long validCycles, long expectedCycles, BigDecimal percent) {
    }

    public record TheoreticalPerformance(BigDecimal strategyReturnPercent, BigDecimal buyAndHoldReturnPercent,
            BigDecimal differencePercent, List<Trade> trades) {
    }

    public record Trade(OffsetDateTime buyTime, BigDecimal buyPrice, OffsetDateTime sellTime, BigDecimal sellPrice,
            BigDecimal returnPercent, boolean open) {
    }

    public record DailySignals(LocalDate date, long buy, long sell, long hold) {
    }
}
