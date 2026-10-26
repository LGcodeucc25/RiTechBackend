package com.ritech.backend.market;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ritech.backend.alert.Alert;
import com.ritech.backend.alert.AlertRepository;
import com.ritech.backend.analysis.Analysis;
import com.ritech.backend.analysis.RecommendationEngine;
import com.ritech.backend.common.DateTimes;
import com.ritech.backend.integration.IntegrationLogService;

/**
 * One cycle reads the latest completed 5-minute window, stores it in dato_mercado and emits one alert. Failures are
 * stored in log_integracion and produce no alert.
 */
@Service
public class MarketCycleService {

    public static final String OPEN_CONDITION = "ABIERTO";
    static final Duration ON_DEMAND_RETRY_INTERVAL = Duration.ofSeconds(60);
    private static final long ON_DEMAND_LOCK_WAIT_SECONDS = 30;

    private static final Logger log = LoggerFactory.getLogger(MarketCycleService.class);

    public enum Outcome {
        STORED, ALREADY_PROCESSED, OUTSIDE_HOURS, FAILED, BUSY, SKIPPED
    }

    /** The last failure since the last success, used to report the data status. */
    public record Failure(String status, String reason, Instant at) {
    }

    private final YahooFinanceClient client;
    private final MarketDataRepository marketDataRepository;
    private final AlertRepository alertRepository;
    private final IntegrationLogService integrationLogService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile Instant lastAttempt;
    private volatile Failure lastFailure;

    public MarketCycleService(YahooFinanceClient client, MarketDataRepository marketDataRepository,
            AlertRepository alertRepository, IntegrationLogService integrationLogService,
            TransactionTemplate transactionTemplate, Clock clock) {
        this.client = client;
        this.marketDataRepository = marketDataRepository;
        this.alertRepository = alertRepository;
        this.integrationLogService = integrationLogService;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /** Called by the scheduler; skips if another cycle is running. */
    public Outcome runScheduled() {
        if (!lock.tryLock()) {
            return Outcome.BUSY;
        }
        try {
            return runLocked();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs the cycle synchronously when the market is open and the last stored window is older than 5 minutes,
     * because the free hosting plan sleeps and the scheduler does not run while it sleeps.
     */
    public Outcome refreshIfStale() {
        Instant now = clock.instant();
        if (!MarketHours.isCycleTime(now)) {
            return Outcome.OUTSIDE_HOURS;
        }
        if (isUpToDate(now)) {
            return Outcome.ALREADY_PROCESSED;
        }
        Instant attempt = lastAttempt;
        if (attempt != null && attempt.plus(ON_DEMAND_RETRY_INTERVAL).isAfter(now)) {
            return Outcome.SKIPPED;
        }
        try {
            if (!lock.tryLock(ON_DEMAND_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                return Outcome.BUSY;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Outcome.BUSY;
        }
        try {
            return isUpToDate(clock.instant()) ? Outcome.ALREADY_PROCESSED : runLocked();
        } finally {
            lock.unlock();
        }
    }

    public Failure lastFailure() {
        return lastFailure;
    }

    private boolean isUpToDate(Instant now) {
        LocalDateTime threshold = DateTimes.toUtc(now.minus(MarketHours.BAR));
        return marketDataRepository.findFirstByOrderByWindowEndDescIdDesc()
                .map(latest -> latest.getWindowEnd() != null && latest.getWindowEnd().isAfter(threshold))
                .orElse(false);
    }

    private Outcome runLocked() {
        Instant now = clock.instant();
        if (!MarketHours.isCycleTime(now)) {
            return Outcome.OUTSIDE_HOURS;
        }
        lastAttempt = now;
        MarketSnapshot snapshot;
        try {
            snapshot = MarketSnapshot.evaluate(client.fetchChart(), now);
        } catch (MarketDataException ex) {
            integrationLogService.recordFailure(ex.getStatus(), ex.getMessage());
            lastFailure = new Failure(ex.getStatus(), ex.getMessage(), now);
            return Outcome.FAILED;
        }
        LocalDateTime windowEnd = DateTimes.toUtc(snapshot.windowEnd());
        if (marketDataRepository.existsByWindowEnd(windowEnd)) {
            lastFailure = null;
            return Outcome.ALREADY_PROCESSED;
        }
        Analysis analysis = RecommendationEngine.analyze(snapshot.rsi(),
                snapshot.windowEnd().atZone(DateTimes.NEW_YORK));
        transactionTemplate.executeWithoutResult(status -> {
            MarketData data = marketDataRepository.save(new MarketData(
                    snapshot.lastClose().setScale(2, RoundingMode.HALF_UP),
                    snapshot.changePercent(),
                    snapshot.rsi().setScale(4, RoundingMode.HALF_UP),
                    OPEN_CONDITION,
                    windowEnd));
            alertRepository.save(new Alert(data.getId(), analysis.recommendation(), analysis.storedCondition(),
                    analysis.rsi(), analysis.explanation(), windowEnd, RecommendationEngine.RULE));
        });
        lastFailure = null;
        log.info("Stored window ending {} with RSI {} and recommendation {}", snapshot.windowEnd(), analysis.rsi(),
                analysis.recommendation());
        return Outcome.STORED;
    }
}
