package com.ritech.backend.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketCycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketCycleScheduler.class);

    private final MarketCycleService cycleService;

    public MarketCycleScheduler(MarketCycleService cycleService) {
        this.cycleService = cycleService;
    }

    @Scheduled(cron = "0 */5 * * * MON-FRI", zone = "America/New_York")
    public void run() {
        try {
            MarketCycleService.Outcome outcome = cycleService.runScheduled();
            log.debug("Scheduled market cycle finished: {}", outcome);
        } catch (RuntimeException ex) {
            log.error("Scheduled market cycle failed", ex);
        }
    }
}
