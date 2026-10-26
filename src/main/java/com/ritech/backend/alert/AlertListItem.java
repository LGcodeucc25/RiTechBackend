package com.ritech.backend.alert;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.analysis.Volatility;
import com.ritech.backend.common.DateTimes;

public record AlertListItem(Integer id, Recommendation type, String condition, BigDecimal rsi, String reason,
        OffsetDateTime time, boolean volatility) {

    public static AlertListItem from(Alert alert) {
        return new AlertListItem(alert.getId(), alert.getRecommendation(), alert.getDetectedCondition(),
                alert.getIndicatorValue(), alert.getJustification(), DateTimes.toBogota(alert.getEmittedAt()),
                alert.getEmittedAt() != null
                        && Volatility.of(DateTimes.toNewYork(alert.getEmittedAt()).toZonedDateTime()).isHigh());
    }
}
