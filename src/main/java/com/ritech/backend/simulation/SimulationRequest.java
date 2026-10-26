package com.ritech.backend.simulation;

/**
 * Fields are read as text so that every field error can be reported at once, including non-numeric values.
 */
public record SimulationRequest(String capital, String variation) {
}
