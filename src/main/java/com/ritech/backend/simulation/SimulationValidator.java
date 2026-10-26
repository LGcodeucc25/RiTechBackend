package com.ritech.backend.simulation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ritech.backend.common.ApiException;
import com.ritech.backend.common.FieldErrorDto;

public final class SimulationValidator {

    /** The largest value that fits in numeric(12,2). */
    public static final BigDecimal MAX_CAPITAL = new BigDecimal("9999999999.99");
    private static final BigDecimal MAX_VARIATION = BigDecimal.valueOf(100);
    private static final BigDecimal MIN_VARIATION = BigDecimal.valueOf(-100);

    private SimulationValidator() {
    }

    public static ValidInput validate(SimulationRequest request) {
        List<FieldErrorDto> errors = new ArrayList<>();
        BigDecimal capital = parse(request == null ? null : request.capital(), errors,
                new FieldErrorDto("capital", "CAPITAL_REQUIRED", "El capital es obligatorio."),
                new FieldErrorDto("capital", "CAPITAL_MUST_BE_NUMERIC", "El capital debe ser un número."));
        if (capital != null) {
            if (capital.signum() <= 0) {
                errors.add(new FieldErrorDto("capital", "CAPITAL_MUST_BE_POSITIVE", "El capital debe ser mayor que cero."));
            } else if (capital.compareTo(MAX_CAPITAL) > 0) {
                errors.add(new FieldErrorDto("capital", "CAPITAL_TOO_LARGE",
                        "El capital no puede superar 9.999.999.999,99."));
            }
            if (decimals(capital) > 2) {
                errors.add(new FieldErrorDto("capital", "CAPITAL_TOO_MANY_DECIMALS",
                        "El capital admite como máximo 2 decimales."));
            }
        }

        BigDecimal variation = parse(request == null ? null : request.variation(), errors,
                new FieldErrorDto("variation", "VARIATION_REQUIRED", "La variación es obligatoria."),
                new FieldErrorDto("variation", "VARIATION_MUST_BE_NUMERIC", "La variación debe ser un número."));
        if (variation != null) {
            if (variation.compareTo(MIN_VARIATION) < 0 || variation.compareTo(MAX_VARIATION) > 0) {
                errors.add(new FieldErrorDto("variation", "VARIATION_OUT_OF_RANGE",
                        "La variación debe estar entre -100 y 100."));
            }
            if (decimals(variation) > 2) {
                errors.add(new FieldErrorDto("variation", "VARIATION_TOO_MANY_DECIMALS",
                        "La variación admite como máximo 2 decimales."));
            }
        }
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        return new ValidInput(capital, variation);
    }

    private static BigDecimal parse(String raw, List<FieldErrorDto> errors, FieldErrorDto requiredError,
            FieldErrorDto numericError) {
        if (raw == null || raw.isBlank()) {
            errors.add(requiredError);
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            errors.add(numericError);
            return null;
        }
    }

    private static int decimals(BigDecimal value) {
        return Math.max(value.stripTrailingZeros().scale(), 0);
    }

    public record ValidInput(BigDecimal capital, BigDecimal variation) {
    }
}
