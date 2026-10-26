package com.ritech.backend.alert;

import java.time.OffsetDateTime;

import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.common.DateTimes;

public record AlertSummary(Integer id, Recommendation type, String reason, OffsetDateTime time) {

    public static AlertSummary from(Alert alert) {
        return alert == null ? null : new AlertSummary(alert.getId(), alert.getRecommendation(),
                alert.getJustification(), DateTimes.toBogota(alert.getEmittedAt()));
    }
}
