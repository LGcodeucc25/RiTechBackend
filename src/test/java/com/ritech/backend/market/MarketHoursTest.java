package com.ritech.backend.market;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MarketHoursTest {

    @ParameterizedTest(name = "{0} NY -> {1}")
    @CsvSource({
            "09:29:59, PRE_MARKET", "09:30:00, OPEN", "12:00:00, OPEN", "15:59:59, OPEN", "16:00:00, CLOSED",
            "20:00:00, CLOSED"
    })
    @DisplayName("SCRUM-67 CA-8: market status by New York time")
    void status(String time, MarketStatus expected) {
        assertThat(MarketHours.status(Fixtures.ny(time))).isEqualTo(expected);
    }

    @Test
    @DisplayName("SCRUM-153 CA-6: the market is closed on weekends")
    void weekend() {
        Instant saturdayNoon = ZonedDateTime.parse("2026-10-24T12:00:00-04:00[America/New_York]").toInstant();
        assertThat(MarketHours.status(saturdayNoon)).isEqualTo(MarketStatus.CLOSED);
        assertThat(MarketHours.isCycleTime(saturdayNoon)).isFalse();
    }

    @Test
    @DisplayName("Market hours follow New York daylight saving time")
    void daylightSavingTime() {
        // 13:35 UTC is 09:35 EDT in October but 08:35 EST in December.
        assertThat(MarketHours.status(Instant.parse("2026-10-23T13:35:00Z"))).isEqualTo(MarketStatus.OPEN);
        assertThat(MarketHours.status(Instant.parse("2026-12-04T13:35:00Z"))).isEqualTo(MarketStatus.PRE_MARKET);
        assertThat(MarketHours.status(Instant.parse("2026-12-04T14:35:00Z"))).isEqualTo(MarketStatus.OPEN);
    }

    @ParameterizedTest(name = "{0} NY -> {1}")
    @CsvSource({"09:30:00, false", "09:35:00, true", "16:00:00, true", "16:05:00, false"})
    @DisplayName("Cycles run from the first completed bar until the 16:00 close")
    void cycleTime(String time, boolean expected) {
        assertThat(MarketHours.isCycleTime(Fixtures.ny(time))).isEqualTo(expected);
    }

    @Test
    @DisplayName("The next cycle after Friday's close is Monday at 9:35")
    void nextCycle() {
        assertThat(MarketHours.nextCycleAt(Fixtures.ny("16:02:00")).toString())
                .isEqualTo("2026-10-26T09:35-04:00[America/New_York]");
        assertThat(MarketHours.nextCycleAt(Fixtures.ny("11:06:30")).toLocalTime().toString()).isEqualTo("11:10");
    }
}
