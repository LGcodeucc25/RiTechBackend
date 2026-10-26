package com.ritech.backend.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ritech.backend.common.DateTimes;

class VolatilityTest {

    @ParameterizedTest(name = "window ending {0} NY -> {1}")
    @CsvSource({
            "09:35, OPENING", "09:45, OPENING", "09:50, NONE", "12:00, NONE",
            "15:45, NONE", "15:50, CLOSING", "16:00, CLOSING"
    })
    @DisplayName("SCRUM-67 CA-9: volatility windows are the first and last 15 minutes of the session")
    void windows(String time, Volatility expected) {
        ZonedDateTime end = ZonedDateTime.parse("2026-10-23T" + time + ":00-04:00[America/New_York]");
        assertThat(Volatility.of(end)).isEqualTo(expected);
    }

    @Test
    @DisplayName("SCRUM-67 CA-9: volatility windows follow New York daylight saving time")
    void daylightSavingTime() {
        // 13:40 UTC is 09:40 in New York during EDT (October) and 08:40 during EST (December).
        ZonedDateTime summer = Instant.parse("2026-10-23T13:40:00Z").atZone(ZoneOffset.UTC);
        ZonedDateTime winter = Instant.parse("2026-12-04T13:40:00Z").atZone(ZoneOffset.UTC);
        assertThat(Volatility.of(summer)).isEqualTo(Volatility.OPENING);
        assertThat(Volatility.of(winter)).isEqualTo(Volatility.NONE);
        ZonedDateTime winterOpening = Instant.parse("2026-12-04T14:40:00Z").atZone(DateTimes.BOGOTA);
        assertThat(Volatility.of(winterOpening)).isEqualTo(Volatility.OPENING);
    }
}
