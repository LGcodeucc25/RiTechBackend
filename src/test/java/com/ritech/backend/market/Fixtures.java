package com.ritech.backend.market;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

final class Fixtures {

    private Fixtures() {
    }

    static String json(String name) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/yahoo/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** The fixture as fetched on 2026-10-23 at 11:06 New York time. */
    static ChartData chart(String name) {
        return new YahooFinanceClient(null, Clock.systemUTC(), Duration.ZERO).parse(json(name), ny("11:06:00"));
    }

    /** An instant on Friday 2026-10-23 (EDT) at the given New York time. */
    static Instant ny(String time) {
        return ZonedDateTime.parse("2026-10-23T" + time + "-04:00[America/New_York]").toInstant();
    }
}
