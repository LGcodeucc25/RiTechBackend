package com.ritech.backend.integration;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ritech.backend.common.DateTimes;

@Service
public class IntegrationLogService {

    public static final String YAHOO_FINANCE = "YAHOO_FINANCE";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_INSUFFICIENT_DATA = "DATOS_INSUFICIENTES";

    private static final Logger log = LoggerFactory.getLogger(IntegrationLogService.class);

    private final IntegrationLogRepository repository;
    private final Clock clock;

    public IntegrationLogService(IntegrationLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void recordFailure(String status, String reason) {
        log.warn("Market cycle without recommendation: {} - {}", status, reason);
        try {
            repository.save(new IntegrationLog(YAHOO_FINANCE, status, reason, DateTimes.nowUtc(clock)));
        } catch (RuntimeException ex) {
            log.error("Could not store integration log entry", ex);
        }
    }
}
