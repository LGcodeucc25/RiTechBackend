package com.ritech.backend.market;

import com.ritech.backend.integration.IntegrationLogService;

/**
 * The market source could not provide usable data; the reason is stored in Spanish in log_integracion.
 */
public class MarketDataException extends RuntimeException {

    private final String status;
    private final boolean stale;

    private MarketDataException(String status, String reason, boolean stale) {
        super(reason);
        this.status = status;
        this.stale = stale;
    }

    public static MarketDataException error(String reason) {
        return new MarketDataException(IntegrationLogService.STATUS_ERROR, reason, false);
    }

    /** The source answered, but its latest data is too old to use. */
    public static MarketDataException stale(String reason) {
        return new MarketDataException(IntegrationLogService.STATUS_ERROR, reason, true);
    }

    public static MarketDataException insufficient(String reason) {
        return new MarketDataException(IntegrationLogService.STATUS_INSUFFICIENT_DATA, reason, false);
    }

    public String getStatus() {
        return status;
    }

    public boolean isStale() {
        return stale;
    }

    public boolean isInsufficientData() {
        return IntegrationLogService.STATUS_INSUFFICIENT_DATA.equals(status);
    }
}
