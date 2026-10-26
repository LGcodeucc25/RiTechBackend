package com.ritech.backend.market;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ritech.backend.alert.Alert;
import com.ritech.backend.alert.AlertRepository;
import com.ritech.backend.alert.AlertSummary;
import com.ritech.backend.analysis.RecommendationEngine;
import com.ritech.backend.analysis.RsiCalculator;
import com.ritech.backend.analysis.Volatility;
import com.ritech.backend.common.ApiException;
import com.ritech.backend.common.DateTimes;
import com.ritech.backend.integration.IntegrationLogService;

@Service
public class MarketService {

    static final String MSG_OK_OPEN = "Datos actualizados.";
    static final String MSG_OK_PRE_MARKET =
            "El mercado aún no abre (preapertura). Se muestran los datos del último cierre.";
    static final String MSG_OK_CLOSED = "El mercado está cerrado. Se muestran los datos del último cierre.";
    static final String MSG_STALE = "Los datos tienen más de 10 minutos de antigüedad. "
            + "No se emiten recomendaciones con datos desactualizados.";
    static final String MSG_SOURCE_ERROR =
            "No fue posible consultar Yahoo Finance. Se muestran los últimos datos disponibles.";
    static final String MSG_INSUFFICIENT = "Aún no hay suficientes datos para calcular el RSI "
            + "(se necesitan 15 cierres de 5 minutos).";

    private static final Logger log = LoggerFactory.getLogger(MarketService.class);

    private final MarketCycleService cycleService;
    private final MarketDataProvider provider;
    private final MarketDataRepository marketDataRepository;
    private final AlertRepository alertRepository;
    private final Clock clock;

    public MarketService(MarketCycleService cycleService, MarketDataProvider provider,
            MarketDataRepository marketDataRepository, AlertRepository alertRepository, Clock clock) {
        this.cycleService = cycleService;
        this.provider = provider;
        this.marketDataRepository = marketDataRepository;
        this.alertRepository = alertRepository;
        this.clock = clock;
    }

    public MarketSummaryResponse summary() {
        try {
            cycleService.refreshIfStale();
        } catch (RuntimeException ex) {
            log.error("On-demand market cycle failed", ex);
        }
        Instant now = clock.instant();
        MarketDataProvider.LiveChart live = provider.liveChart();
        Optional<MarketQuote> quote = live.chart().flatMap(MarketQuote::from);
        Optional<MarketData> latest = marketDataRepository.findFirstByOrderByWindowEndDescIdDesc();
        Optional<Alert> alert = alertRepository.findFirstByOrderByEmittedAtDescIdDesc();
        if (quote.isEmpty() && latest.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MARKET_SOURCE_UNAVAILABLE",
                    "No fue posible obtener datos del NASDAQ 100. Intenta de nuevo en unos minutos.");
        }

        BigDecimal indexValue;
        BigDecimal changePercent;
        BigDecimal changePoints;
        BigDecimal previousClose;
        Instant lastUpdate;
        if (quote.isPresent()) {
            MarketQuote q = quote.get();
            indexValue = q.lastClose();
            changePercent = q.changePercent();
            changePoints = q.changePoints();
            previousClose = q.previousClose();
            lastUpdate = q.time();
        } else {
            MarketData data = latest.get();
            indexValue = data.getIndexValue();
            changePercent = data.getChangePercent();
            previousClose = indexValue.divide(BigDecimal.ONE.add(changePercent.movePointLeft(2)), MathContext.DECIMAL64);
            changePoints = indexValue.subtract(previousClose);
            lastUpdate = DateTimes.toInstant(data.getWindowEnd());
        }

        BigDecimal rsi = alert.map(Alert::getIndicatorValue).orElse(null);
        String condition = alert.map(Alert::getDetectedCondition).orElse(null);
        if (alert.isEmpty() && live.chart().isPresent()) {
            List<BigDecimal> closes = MarketQuote.sessionBars(live.chart().get()).stream()
                    .map(ChartData.Bar::close).toList();
            Optional<BigDecimal> liveRsi = RsiCalculator.latest(closes).map(RecommendationEngine::round);
            rsi = liveRsi.orElse(null);
            condition = liveRsi.map(v -> RecommendationEngine.condition(RecommendationEngine.classify(v)))
                    .orElse(null);
        }

        MarketStatus marketStatus = MarketHours.status(now);
        DataStatus dataStatus = dataStatus(now, live, latest);
        return new MarketSummaryResponse(scale2(indexValue), scale2(changePercent), scale2(changePoints),
                scale2(previousClose), marketStatus, DateTimes.toBogota(lastUpdate),
                MarketHours.nextCycleAt(now).withZoneSameInstant(DateTimes.BOGOTA).toOffsetDateTime(),
                rsi == null ? null : scale2(rsi), condition, alert.map(AlertSummary::from).orElse(null), dataStatus,
                message(dataStatus, marketStatus));
    }

    public List<ChartPoint> history(String range) {
        boolean oneDay;
        if ("1d".equals(range) || range == null || range.isBlank()) {
            oneDay = true;
        } else if ("5d".equals(range)) {
            oneDay = false;
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "El rango debe ser 1d o 5d.");
        }
        Optional<ChartData> chart = provider.liveChart().chart();
        if (chart.isPresent()) {
            List<ChartData.Bar> bars = MarketQuote.sessionBars(chart.get());
            if (!bars.isEmpty()) {
                LocalDate lastDay = nyDate(bars.getLast().start());
                return fromBars(bars, bar -> !oneDay || nyDate(bar.start()).equals(lastDay));
            }
        }
        Instant now = clock.instant();
        return fromStored(DateTimes.toUtc(now.minus(Duration.ofDays(oneDay ? 1 : 7))), DateTimes.toUtc(now));
    }

    /** Chart points for one New York trading day, used by the alert detail. */
    public List<ChartPoint> pointsForDay(LocalDate nyDate) {
        Optional<ChartData> chart = provider.liveChart().chart();
        if (chart.isPresent()) {
            List<ChartData.Bar> bars = MarketQuote.sessionBars(chart.get());
            if (bars.stream().anyMatch(bar -> nyDate(bar.start()).equals(nyDate))) {
                return fromBars(bars, bar -> nyDate(bar.start()).equals(nyDate));
            }
        }
        LocalDateTime from = DateTimes.toUtc(nyDate.atStartOfDay(DateTimes.NEW_YORK));
        return fromStored(from, from.plusDays(1));
    }

    private List<ChartPoint> fromBars(List<ChartData.Bar> bars, Predicate<ChartData.Bar> include) {
        List<BigDecimal> rsiSeries = RsiCalculator.series(bars.stream().map(ChartData.Bar::close).toList());
        LocalDateTime from = DateTimes.toUtc(bars.getFirst().end());
        LocalDateTime to = DateTimes.toUtc(bars.getLast().end());
        Map<LocalDateTime, Alert> alerts = alertRepository.findByEmittedAtBetweenOrderByEmittedAtAsc(from, to).stream()
                .collect(Collectors.toMap(Alert::getEmittedAt, Function.identity(), (a, b) -> b));
        Set<LocalDateTime> processed = marketDataRepository.findByWindowEndBetweenOrderByWindowEndAsc(from, to).stream()
                .map(MarketData::getWindowEnd).collect(Collectors.toSet());

        List<ChartPoint> points = new ArrayList<>();
        for (int i = 0; i < bars.size(); i++) {
            ChartData.Bar bar = bars.get(i);
            if (!include.test(bar)) {
                continue;
            }
            LocalDateTime end = DateTimes.toUtc(bar.end());
            BigDecimal rsi = i >= RsiCalculator.PERIOD ? rsiSeries.get(i - RsiCalculator.PERIOD) : null;
            Alert alert = alerts.get(end);
            points.add(new ChartPoint(DateTimes.toBogota(bar.end()), scale2(bar.close()),
                    rsi == null ? null : RecommendationEngine.round(rsi),
                    alert == null ? null : alert.getRecommendation(),
                    Volatility.of(bar.end().atZone(DateTimes.NEW_YORK)),
                    processed.contains(end) ? ChartPoint.PROCESSED : ChartPoint.NOT_PROCESSED));
        }
        return points;
    }

    private List<ChartPoint> fromStored(LocalDateTime from, LocalDateTime to) {
        List<MarketData> data = marketDataRepository.findByWindowEndBetweenOrderByWindowEndAsc(from, to);
        Map<Integer, Alert> alerts = data.isEmpty() ? Map.of()
                : alertRepository.findByMarketDataIdIn(data.stream().map(MarketData::getId).toList()).stream()
                        .collect(Collectors.toMap(Alert::getMarketDataId, Function.identity(), (a, b) -> b));
        return data.stream().map(d -> {
            Alert alert = alerts.get(d.getId());
            Instant end = DateTimes.toInstant(d.getWindowEnd());
            return new ChartPoint(DateTimes.toBogota(d.getWindowEnd()), scale2(d.getIndexValue()),
                    d.getRsi() == null ? null : RecommendationEngine.round(d.getRsi()),
                    alert == null ? null : alert.getRecommendation(),
                    Volatility.of(end.atZone(DateTimes.NEW_YORK)), ChartPoint.PROCESSED);
        }).toList();
    }

    private DataStatus dataStatus(Instant now, MarketDataProvider.LiveChart live, Optional<MarketData> latest) {
        if (live.error() != null) {
            return live.error().isInsufficientData() ? DataStatus.INSUFFICIENT_DATA : DataStatus.SOURCE_ERROR;
        }
        if (!MarketHours.isCycleTime(now)) {
            return DataStatus.OK;
        }
        MarketCycleService.Failure failure = cycleService.lastFailure();
        if (failure != null && IntegrationLogService.STATUS_INSUFFICIENT_DATA.equals(failure.status())) {
            return DataStatus.INSUFFICIENT_DATA;
        }
        LocalDateTime freshness = DateTimes.toUtc(now.minus(MarketSnapshot.MAX_AGE));
        boolean fresh = latest.map(d -> d.getWindowEnd() != null && !d.getWindowEnd().isBefore(freshness))
                .orElse(false);
        if (fresh) {
            return DataStatus.OK;
        }
        return failure != null && !failure.stale() ? DataStatus.SOURCE_ERROR : DataStatus.STALE;
    }

    private static String message(DataStatus dataStatus, MarketStatus marketStatus) {
        return switch (dataStatus) {
            case OK -> switch (marketStatus) {
                case OPEN -> MSG_OK_OPEN;
                case PRE_MARKET -> MSG_OK_PRE_MARKET;
                case CLOSED -> MSG_OK_CLOSED;
            };
            case STALE -> MSG_STALE;
            case SOURCE_ERROR -> MSG_SOURCE_ERROR;
            case INSUFFICIENT_DATA -> MSG_INSUFFICIENT;
        };
    }

    private static LocalDate nyDate(Instant instant) {
        return instant.atZone(DateTimes.NEW_YORK).toLocalDate();
    }

    private static BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
