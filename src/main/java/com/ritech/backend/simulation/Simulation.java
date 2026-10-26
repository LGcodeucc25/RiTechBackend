package com.ritech.backend.simulation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "simulacion")
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_simulacion")
    private Integer id;

    @Column(name = "id_usuario", nullable = false)
    private Integer userId;

    @Column(name = "capital_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal initialCapital;

    @Column(name = "variacion_nasdaq", nullable = false, precision = 8, scale = 4)
    private BigDecimal nasdaqVariation;

    @Column(name = "resultado_porcentaje", precision = 6, scale = 2)
    private BigDecimal resultPercent;

    @Column(name = "resultado_monetario", precision = 12, scale = 2)
    private BigDecimal resultAmount;

    @Column(name = "valor_final", precision = 12, scale = 2)
    private BigDecimal finalValue;

    /** UTC. */
    @Column(name = "fecha_hora")
    private LocalDateTime createdAt;

    protected Simulation() {
    }

    public Simulation(Integer userId, BigDecimal initialCapital, BigDecimal nasdaqVariation,
            BigDecimal resultPercent, BigDecimal resultAmount, BigDecimal finalValue, LocalDateTime createdAt) {
        this.userId = userId;
        this.initialCapital = initialCapital;
        this.nasdaqVariation = nasdaqVariation;
        this.resultPercent = resultPercent;
        this.resultAmount = resultAmount;
        this.finalValue = finalValue;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public BigDecimal getInitialCapital() {
        return initialCapital;
    }

    public BigDecimal getNasdaqVariation() {
        return nasdaqVariation;
    }

    public BigDecimal getResultPercent() {
        return resultPercent;
    }

    public BigDecimal getResultAmount() {
        return resultAmount;
    }

    public BigDecimal getFinalValue() {
        return finalValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
