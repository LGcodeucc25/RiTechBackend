package com.ritech.backend.analysis;

public enum Recommendation {
    BUY("COMPRAR"),
    SELL("VENDER"),
    HOLD("MANTENER");

    private final String databaseValue;

    Recommendation(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static Recommendation fromDatabaseValue(String value) {
        for (Recommendation recommendation : values()) {
            if (recommendation.databaseValue.equalsIgnoreCase(value)) {
                return recommendation;
            }
        }
        throw new IllegalArgumentException("Unknown recommendation value: " + value);
    }
}
