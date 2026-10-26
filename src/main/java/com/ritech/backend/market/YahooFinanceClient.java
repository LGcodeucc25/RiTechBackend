package com.ritech.backend.market;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads NASDAQ 100 5-minute bars from Yahoo Finance's unofficial chart endpoint. Data is for personal and academic use
 * under Yahoo's terms and is not redistributed.
 */
public class YahooFinanceClient {

    public static final URI CHART_URI = URI.create(
            "https://query1.finance.yahoo.com/v8/finance/chart/%5ENDX?range=5d&interval=5m&includePrePost=false");
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/130.0 Safari/537.36";
    static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceClient.class);

    private final RestClient restClient;
    private final Clock clock;
    private final Duration retryDelay;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private ChartData cached;

    public YahooFinanceClient(RestClient restClient, Clock clock, Duration retryDelay) {
        this.restClient = restClient;
        this.clock = clock;
        this.retryDelay = retryDelay;
    }

    /** Returns the chart, from a 60-second cache when possible. */
    public synchronized ChartData fetchChart() {
        Instant now = clock.instant();
        if (cached != null && cached.fetchedAt().plus(CACHE_TTL).isAfter(now)) {
            return cached;
        }
        ChartData chart = parse(download(), now);
        cached = chart;
        return chart;
    }

    /** The last successful response, even if older than the cache lifetime, or null. */
    public synchronized ChartData lastSuccessful() {
        return cached;
    }

    private String download() {
        for (int attempt = 0; ; attempt++) {
            try {
                String body = restClient.get().uri(CHART_URI).header("User-Agent", USER_AGENT)
                        .retrieve().body(String.class);
                if (body == null || body.isBlank()) {
                    throw MarketDataException.error("Yahoo Finance devolvió una respuesta vacía.");
                }
                return body;
            } catch (RestClientResponseException ex) {
                int code = ex.getStatusCode().value();
                boolean retryable = code == 429 || ex.getStatusCode().is5xxServerError();
                if (retryable && attempt == 0) {
                    log.warn("Yahoo Finance answered HTTP {}, retrying once", code);
                    pause();
                    continue;
                }
                throw MarketDataException.error("Yahoo Finance respondió HTTP " + code + ".");
            } catch (ResourceAccessException ex) {
                throw MarketDataException.error("Sin respuesta de Yahoo Finance (tiempo de espera agotado o sin conexión).");
            } catch (RestClientException ex) {
                throw MarketDataException.error("Error al consultar Yahoo Finance.");
            }
        }
    }

    private void pause() {
        if (retryDelay.isZero()) {
            return;
        }
        try {
            Thread.sleep(retryDelay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw MarketDataException.error("Consulta a Yahoo Finance interrumpida.");
        }
    }

    ChartData parse(String body, Instant fetchedAt) {
        JsonNode root;
        try {
            root = jsonMapper.readTree(body);
        } catch (JacksonException ex) {
            throw MarketDataException.error("Respuesta de Yahoo Finance con formato JSON inválido.");
        }
        JsonNode result = root.path("chart").path("result").path(0);
        if (!result.isObject()) {
            throw MarketDataException.error("Respuesta de Yahoo Finance sin resultados.");
        }
        JsonNode timestamps = result.path("timestamp");
        JsonNode closes = result.path("indicators").path("quote").path(0).path("close");
        if (!timestamps.isArray() || !closes.isArray() || timestamps.size() != closes.size()) {
            throw MarketDataException.insufficient("Respuesta de Yahoo Finance sin serie de precios completa.");
        }
        List<ChartData.Bar> bars = new ArrayList<>(timestamps.size());
        for (int i = 0; i < timestamps.size(); i++) {
            JsonNode ts = timestamps.get(i);
            if (!ts.isNumber()) {
                throw MarketDataException.error("Respuesta de Yahoo Finance con marcas de tiempo inválidas.");
            }
            bars.add(new ChartData.Bar(Instant.ofEpochSecond(ts.longValue()), decimal(closes.get(i))));
        }
        JsonNode meta = result.path("meta");
        return new ChartData(bars, decimal(meta.path("regularMarketPrice")), decimal(meta.path("previousClose")),
                decimal(meta.path("chartPreviousClose")), fetchedAt);
    }

    private static BigDecimal decimal(JsonNode node) {
        return node != null && node.isNumber() ? node.decimalValue() : null;
    }
}
