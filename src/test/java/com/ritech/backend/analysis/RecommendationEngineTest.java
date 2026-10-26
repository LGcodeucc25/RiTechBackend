package com.ritech.backend.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.ritech.backend.common.DateTimes;

class RecommendationEngineTest {

    private static final ZonedDateTime MIDDAY = ZonedDateTime.of(LocalDateTime.parse("2026-10-23T12:00:00"),
            DateTimes.NEW_YORK);

    private static Analysis analyze(String rsi) {
        return RecommendationEngine.analyze(new BigDecimal(rsi), MIDDAY);
    }

    @ParameterizedTest(name = "RSI {0} -> {1}")
    @CsvSource({
            "29.99, BUY", "30.00, BUY", "30.004, BUY", "30.01, HOLD", "50, HOLD",
            "69.99, HOLD", "69.995, SELL", "70.00, SELL", "85.3, SELL", "0, BUY", "100, SELL"
    })
    @DisplayName("SCRUM-67 CA-4: classification uses the RSI rounded to 2 decimals with inclusive limits")
    void classification(String rsi, Recommendation expected) {
        assertThat(analyze(rsi).recommendation()).isEqualTo(expected);
    }

    @Test
    @DisplayName("SCRUM-67 CA-4: a value that rounds to 70.00 is SELL and shows 70,00 at the limit")
    void roundsToSeventy() {
        Analysis analysis = analyze("69.996");
        assertThat(analysis.rsi()).isEqualByComparingTo("70.00");
        assertThat(analysis.recommendation()).isEqualTo(Recommendation.SELL);
        assertThat(analysis.explanation()).startsWith("El RSI alcanzó 70,00, justo en el límite de 70.");
    }

    @Test
    @DisplayName("SCRUM-152 CA-1: SELL explanation")
    void sellExplanation() {
        assertThat(analyze("72.0612").explanation()).isEqualTo("El RSI alcanzó 72,06, por encima del límite de 70. "
                + "El índice se considera sobrecomprado, por lo que se recomienda VENDER.");
    }

    @Test
    @DisplayName("SCRUM-152 CA-2: BUY explanation, including the exact limit of 30")
    void buyExplanation() {
        assertThat(analyze("25.5").explanation()).isEqualTo("El RSI bajó a 25,50, por debajo del límite de 30. "
                + "El índice se considera sobrevendido, por lo que se recomienda COMPRAR.");
        assertThat(analyze("30").explanation()).startsWith("El RSI bajó a 30,00, justo en el límite de 30.");
    }

    @Test
    @DisplayName("SCRUM-152 CA-3: HOLD explanation")
    void holdExplanation() {
        Analysis analysis = analyze("55.123");
        assertThat(analysis.explanation()).isEqualTo("El RSI está en 55,12, entre los límites de 30 y 70. "
                + "El índice no muestra sobrecompra ni sobreventa, por lo que se recomienda MANTENER.");
        assertThat(analysis.condition()).isEqualTo("condición neutra");
        assertThat(analysis.storedCondition()).isEqualTo("condición neutra");
    }

    @Test
    @DisplayName("SCRUM-152 CA-4: a signal in the opening window carries the warning and the stored condition says so")
    void openingWarning() {
        ZonedDateTime windowEnd = ZonedDateTime.of(LocalDateTime.parse("2026-10-23T09:40:00"), DateTimes.NEW_YORK);
        Analysis analysis = RecommendationEngine.analyze(new BigDecimal("75"), windowEnd);
        assertThat(analysis.volatility()).isEqualTo(Volatility.OPENING);
        assertThat(analysis.explanation()).endsWith(RecommendationEngine.OPENING_WARNING);
        assertThat(analysis.storedCondition()).isEqualTo("sobrecompra (alta volatilidad)");
    }

    @Test
    @DisplayName("SCRUM-152 CA-4: a signal in the closing window carries the closing warning")
    void closingWarning() {
        ZonedDateTime windowEnd = ZonedDateTime.of(LocalDateTime.parse("2026-10-23T15:55:00"), DateTimes.NEW_YORK);
        Analysis analysis = RecommendationEngine.analyze(new BigDecimal("20"), windowEnd);
        assertThat(analysis.explanation()).endsWith(RecommendationEngine.CLOSING_WARNING);
        assertThat(analysis.storedCondition()).isEqualTo("sobreventa (alta volatilidad)");
    }
}
