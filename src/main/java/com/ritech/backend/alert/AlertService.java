package com.ritech.backend.alert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.analysis.RecommendationEngine;
import com.ritech.backend.analysis.RsiCalculator;
import com.ritech.backend.analysis.Volatility;
import com.ritech.backend.common.ApiException;
import com.ritech.backend.common.DateTimes;
import com.ritech.backend.common.Filters;
import com.ritech.backend.common.PageResponse;
import com.ritech.backend.market.MarketData;
import com.ritech.backend.market.MarketDataRepository;
import com.ritech.backend.market.MarketHours;
import com.ritech.backend.market.MarketService;

@Service
public class AlertService {

    static final List<Recommendation> DEFAULT_TYPES = List.of(Recommendation.BUY, Recommendation.SELL);
    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;
    static final String NOT_FOUND_MESSAGE = "No se encontró la alerta.";

    private final AlertRepository alertRepository;
    private final MarketDataRepository marketDataRepository;
    private final MarketService marketService;

    public AlertService(AlertRepository alertRepository, MarketDataRepository marketDataRepository,
            MarketService marketService) {
        this.alertRepository = alertRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketService = marketService;
    }

    public PageResponse<AlertListItem> list(String types, LocalDate from, LocalDate to, Integer page, Integer size) {
        Filters.checkRange(from, to);
        int pageNumber = page == null ? 0 : Math.max(page, 0);
        int pageSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        PageRequest pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Order.desc("emittedAt"), Sort.Order.desc("id")));
        return PageResponse.from(alertRepository.findByRecommendationInAndEmittedAtBetween(
                Filters.types(types, DEFAULT_TYPES), Filters.fromDay(from), Filters.toDay(to), pageable),
                AlertListItem::from);
    }

    public AlertDetail detail(Integer id, String receivedBy) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND_MESSAGE));
        MarketData data = marketDataRepository.findById(alert.getMarketDataId()).orElse(null);
        ZonedDateTime emittedNy = DateTimes.toNewYork(alert.getEmittedAt()).toZonedDateTime();
        Volatility volatility = Volatility.of(emittedNy);
        BigDecimal rsi = alert.getIndicatorValue();

        AlertDetail.DataUsed dataUsed = data == null ? null : new AlertDetail.DataUsed(data.getId(),
                data.getIndexValue(), data.getChangePercent().setScale(2, RoundingMode.HALF_UP),
                data.getRsi() == null ? null : RecommendationEngine.round(data.getRsi()),
                DateTimes.toBogota(data.getWindowEnd().minus(MarketHours.BAR)), DateTimes.toBogota(data.getWindowEnd()));
        AlertDetail.Traceability traceability = new AlertDetail.Traceability(dataUsed,
                RsiCalculator.PERIOD + " ventanas de 5 minutos", alert.getAppliedRule(),
                DateTimes.toBogota(alert.getEmittedAt()), receivedBy);

        return new AlertDetail(alert.getId(), alert.getRecommendation(), alert.getDetectedCondition(), rsi,
                DateTimes.toBogota(alert.getEmittedAt()), emittedNy.toOffsetDateTime(), alert.getJustification(),
                RecommendationEngine.warning(volatility), criteria(rsi, volatility), traceability,
                marketService.pointsForDay(emittedNy.toLocalDate()));
    }

    static List<AlertDetail.Criterion> criteria(BigDecimal rsi, Volatility volatility) {
        String observed = rsi == null ? "Sin dato" : "RSI " + RecommendationEngine.format(rsi);
        Recommendation type = rsi == null ? null : RecommendationEngine.classify(rsi);
        String volatilityObserved = switch (volatility) {
            case OPENING -> "No: primeros 15 minutos de negociación";
            case CLOSING -> "No: últimos 15 minutos antes del cierre";
            case NONE -> "Sí";
        };
        return List.of(
                new AlertDetail.Criterion("RSI(14) menor o igual a 30 (sobreventa)", observed,
                        type == Recommendation.BUY),
                new AlertDetail.Criterion("RSI(14) mayor o igual a 70 (sobrecompra)", observed,
                        type == Recommendation.SELL),
                new AlertDetail.Criterion("RSI(14) entre 30 y 70 (condición neutra)", observed,
                        type == Recommendation.HOLD),
                new AlertDetail.Criterion("Datos completos: al menos 15 cierres de 5 minutos", "Sí", true),
                new AlertDetail.Criterion("Fuera de las franjas de alta volatilidad", volatilityObserved,
                        !volatility.isHigh()));
    }
}
