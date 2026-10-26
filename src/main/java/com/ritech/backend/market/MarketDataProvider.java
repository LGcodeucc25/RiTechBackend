package com.ritech.backend.market;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Read-only access to the live chart for display. Never throws: when the source fails it falls back to the last
 * successful response, if any.
 */
@Component
public class MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(MarketDataProvider.class);

    private final YahooFinanceClient client;

    public MarketDataProvider(YahooFinanceClient client) {
        this.client = client;
    }

    public LiveChart liveChart() {
        try {
            return new LiveChart(Optional.of(client.fetchChart()), null);
        } catch (MarketDataException ex) {
            log.warn("Live chart unavailable: {}", ex.getMessage());
            return new LiveChart(Optional.ofNullable(client.lastSuccessful()), ex);
        }
    }

    /**
     * @param chart the chart to show, possibly an older successful one
     * @param error the error of the latest attempt, or null if it succeeded
     */
    public record LiveChart(Optional<ChartData> chart, MarketDataException error) {
    }
}
