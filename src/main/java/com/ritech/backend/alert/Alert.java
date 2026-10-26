package com.ritech.backend.alert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ritech.backend.analysis.Recommendation;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerta")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer id;

    @Column(name = "id_dato", nullable = false)
    private Integer marketDataId;

    @Convert(converter = RecommendationConverter.class)
    @Column(name = "recomendacion", nullable = false, length = 20)
    private Recommendation recommendation;

    @Column(name = "condicion_detectada", length = 100)
    private String detectedCondition;

    @Column(name = "valor_indicador", precision = 12, scale = 2)
    private BigDecimal indicatorValue;

    @Column(name = "justificacion", columnDefinition = "text")
    private String justification;

    /** Emission time (end of the window), UTC. */
    @Column(name = "fecha_hora")
    private LocalDateTime emittedAt;

    @Column(name = "regla_aplicada", length = 50)
    private String appliedRule;

    protected Alert() {
    }

    public Alert(Integer marketDataId, Recommendation recommendation, String detectedCondition,
            BigDecimal indicatorValue, String justification, LocalDateTime emittedAt, String appliedRule) {
        this.marketDataId = marketDataId;
        this.recommendation = recommendation;
        this.detectedCondition = detectedCondition;
        this.indicatorValue = indicatorValue;
        this.justification = justification;
        this.emittedAt = emittedAt;
        this.appliedRule = appliedRule;
    }

    public Integer getId() {
        return id;
    }

    public Integer getMarketDataId() {
        return marketDataId;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    public String getDetectedCondition() {
        return detectedCondition;
    }

    public BigDecimal getIndicatorValue() {
        return indicatorValue;
    }

    public String getJustification() {
        return justification;
    }

    public LocalDateTime getEmittedAt() {
        return emittedAt;
    }

    public String getAppliedRule() {
        return appliedRule;
    }
}
