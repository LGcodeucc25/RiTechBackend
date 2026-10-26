package com.ritech.backend.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.ritech.backend.common.ApiException;
import com.ritech.backend.common.FieldErrorDto;

class SimulationValidatorTest {

    private static List<String> codes(String capital, String variation) {
        try {
            SimulationValidator.validate(new SimulationRequest(capital, variation));
            return List.of();
        } catch (ApiException ex) {
            return ex.getFieldErrors().stream().map(FieldErrorDto::code).toList();
        }
    }

    @Test
    @DisplayName("SCRUM-13 CA-5: all field errors are returned at once")
    void allErrorsAtOnce() {
        assertThat(codes(null, "abc")).containsExactly("CAPITAL_REQUIRED", "VARIATION_MUST_BE_NUMERIC");
    }

    @ParameterizedTest(name = "capital {0} -> {1}")
    @CsvSource({
            "0, CAPITAL_MUST_BE_POSITIVE",
            "-5, CAPITAL_MUST_BE_POSITIVE",
            "10.123, CAPITAL_TOO_MANY_DECIMALS",
            "10000000000, CAPITAL_TOO_LARGE",
            "abc, CAPITAL_MUST_BE_NUMERIC",
            "'  ', CAPITAL_REQUIRED"
    })
    @DisplayName("SCRUM-13: invalid capital values are rejected")
    void invalidCapital(String capital, String code) {
        assertThat(codes(capital, "1")).containsExactly(code);
    }

    @ParameterizedTest(name = "variation {0} -> {1}")
    @CsvSource({
            "100.01, VARIATION_OUT_OF_RANGE",
            "-100.01, VARIATION_OUT_OF_RANGE",
            "1.234, VARIATION_TOO_MANY_DECIMALS",
            "x, VARIATION_MUST_BE_NUMERIC"
    })
    @DisplayName("SCRUM-14: invalid variation values are rejected")
    void invalidVariation(String variation, String code) {
        assertThat(codes("1000", variation)).containsExactly(code);
    }

    @Test
    @DisplayName("SCRUM-14: the limits -100, +100 and the maximum capital are accepted")
    void limitsAccepted() {
        assertThat(codes("9999999999.99", "100")).isEmpty();
        assertThat(codes("0.01", "-100")).isEmpty();
        assertThat(codes("1000.50", "1.50")).isEmpty();
    }

    @Test
    @DisplayName("A missing body reports both fields as required")
    void missingBody() {
        assertThatThrownBy(() -> SimulationValidator.validate(null))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getFieldErrors()).hasSize(2));
    }
}
