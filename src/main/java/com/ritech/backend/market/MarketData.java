package com.ritech.backend.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dato_mercado")
public class MarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dato")
    private Integer id;

    @Column(name = "valor_indice", nullable = false, precision = 12, scale = 2)
    private BigDecimal indexValue;

    @Column(name = "variacion", nullable = false, precision = 8, scale = 4)
    private BigDecimal changePercent;

    @Column(name = "rsi", precision = 8, scale = 4)
    private BigDecimal rsi;

    @Column(name = "condicion_mercado", length = 50)
    private String marketCondition;

    /** End of the 5-minute window, UTC. */
    @Column(name = "fecha_hora")
    private LocalDateTime windowEnd;

    protected MarketData() {
    }

    public MarketData(BigDecimal indexValue, BigDecimal changePercent, BigDecimal rsi, String marketCondition,
            LocalDateTime windowEnd) {
        this.indexValue = indexValue;
        this.changePercent = changePercent;
        this.rsi = rsi;
        this.marketCondition = marketCondition;
        this.windowEnd = windowEnd;
    }

    public Integer getId() {
        return id;
    }

    public BigDecimal getIndexValue() {
        return indexValue;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public BigDecimal getRsi() {
        return rsi;
    }

    public String getMarketCondition() {
        return marketCondition;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }
}
