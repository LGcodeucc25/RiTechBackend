package com.ritech.backend.market;

import com.ritech.backend.integration.IntegrationLogService;

/**
 * The market source could not provide usable data; the reason is stored in Spanish in log_integracion.
 */
public class MarketDataException extends RuntimeException {

    private final String status;

    public MarketDataException(String status, String reason) {
        super(reason);
        this.status = status;
    }

    public static MarketDataException error(String reason) {
        return new MarketDataException(IntegrationLogService.STATUS_ERROR, reason);
    }

    public static MarketDataException insufficient(String reason) {
        return new MarketDataException(IntegrationLogService.STATUS_INSUFFICIENT_DATA, reason);
    }

    public String getStatus() {
        return status;
    }

    public boolean isInsufficientData() {
        return IntegrationLogService.STATUS_INSUFFICIENT_DATA.equals(status);
    }
}
