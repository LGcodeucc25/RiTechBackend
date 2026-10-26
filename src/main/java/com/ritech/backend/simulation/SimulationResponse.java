package com.ritech.backend.simulation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.ritech.backend.alert.AlertSummary;

public record SimulationResponse(
        Integer id,
        BigDecimal capital,
        BigDecimal variation,
        BigDecimal rawPercent,
        BigDecimal resultPercent,
        BigDecimal resultAmount,
        BigDecimal finalValue,
        BigDecimal maxLoss,
        boolean capped,
        String currency,
        OffsetDateTime createdAt,
        boolean saved,
        String warning,
        AlertSummary recommendationAtSimulation) {
}
