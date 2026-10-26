package com.ritech.backend.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.ritech.backend.integration.IntegrationLogService;

class YahooFinanceClientTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Fixtures.ny("11:06:00"));
    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };

    private MockRestServiceServer server;
    private YahooFinanceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new YahooFinanceClient(builder.build(), clock, Duration.ZERO);
    }

    @Test
    @DisplayName("SCRUM-153 CA-1: a successful response is parsed, keeping null closes, with a browser User-Agent")
    void success() {
        server.expect(once(), requestTo(YahooFinanceClient.CHART_URI))
                .andExpect(header("User-Agent", YahooFinanceClient.USER_AGENT))
                .andRespond(withSuccess(Fixtures.json("chart-success.json"), MediaType.APPLICATION_JSON));

        ChartData chart = client.fetchChart();

        assertThat(chart.bars()).hasSize(23);
        assertThat(chart.bars().stream().filter(bar -> bar.close() == null)).hasSize(1);
        assertThat(chart.regularMarketPrice()).isEqualByComparingTo("25133.0");
        assertThat(chart.previousClose()).isEqualByComparingTo("24990.0");
        assertThat(chart.chartPreviousClose()).isEqualByComparingTo("24800.0");
        server.verify();
    }

    @Test
    @DisplayName("A successful response is cached for 60 seconds")
    void cache() {
        server.expect(twice(), requestTo(YahooFinanceClient.CHART_URI))
                .andRespond(withSuccess(Fixtures.json("chart-success.json"), MediaType.APPLICATION_JSON));

        client.fetchChart();
        now.set(now.get().plusSeconds(59));
        client.fetchChart();
        now.set(now.get().plusSeconds(2));
        client.fetchChart();

        server.verify();
    }

    @Test
    @DisplayName("SCRUM-153 CA-3: HTTP 429 is retried once and then succeeds")
    void retryOn429() {
        server.expect(once(), requestTo(YahooFinanceClient.CHART_URI))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(once(), requestTo(YahooFinanceClient.CHART_URI))
                .andRespond(withSuccess(Fixtures.json("chart-success.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchChart().bars()).isNotEmpty();
        server.verify();
    }

    @Test
    @DisplayName("SCRUM-153 CA-3: a second HTTP 429 or 5xx fails with an ERROR reason")
    void failsAfterOneRetry() {
        server.expect(once(), requestTo(YahooFinanceClient.CHART_URI))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(once(), requestTo(YahooFinanceClient.CHART_URI)).andRespond(withServerError());

        assertThatThrownBy(client::fetchChart)
                .isInstanceOfSatisfying(MarketDataException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(IntegrationLogService.STATUS_ERROR);
                    assertThat(ex.getMessage()).isEqualTo("Yahoo Finance respondió HTTP 500.");
                });
        server.verify();
    }

    @Test
    @DisplayName("HTTP 404 is not retried")
    void noRetryOnClientError() {
        server.expect(once(), requestTo(YahooFinanceClient.CHART_URI)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(client::fetchChart).isInstanceOf(MarketDataException.class);
        server.verify();
    }

    @Test
    @DisplayName("SCRUM-153 CA-4: a response without timestamps is reported as insufficient data")
    void incompleteData() {
        server.expect(once(), requestTo(YahooFinanceClient.CHART_URI))
                .andRespond(withSuccess(Fixtures.json("chart-incomplete.json"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::fetchChart)
                .isInstanceOfSatisfying(MarketDataException.class,
                        ex -> assertThat(ex.isInsufficientData()).isTrue());
    }

    @Test
    @DisplayName("SCRUM-153 CA-4: malformed JSON is reported as an error")
    void malformedJson() {
        server.expect(once(), requestTo(YahooFinanceClient.CHART_URI))
                .andRespond(withSuccess("{not json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::fetchChart)
                .isInstanceOfSatisfying(MarketDataException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(IntegrationLogService.STATUS_ERROR));
    }
}
