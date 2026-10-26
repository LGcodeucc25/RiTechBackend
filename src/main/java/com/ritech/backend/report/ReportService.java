package com.ritech.backend.report;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ritech.backend.alert.Alert;
import com.ritech.backend.alert.AlertRepository;
import com.ritech.backend.analysis.Recommendation;
import com.ritech.backend.analysis.Volatility;
import com.ritech.backend.audit.AuditAction;
import com.ritech.backend.audit.AuditService;
import com.ritech.backend.common.ApiException;
import com.ritech.backend.common.DateTimes;
import com.ritech.backend.common.Filters;
import com.ritech.backend.integration.IntegrationLog;
import com.ritech.backend.integration.IntegrationLogRepository;
import com.ritech.backend.integration.IntegrationLogService;
import com.ritech.backend.market.MarketData;
import com.ritech.backend.market.MarketDataRepository;

@Service
public class ReportService {

    private static final List<Recommendation> ALL_TYPES = List.of(Recommendation.values());

    private final AlertRepository alertRepository;
    private final MarketDataRepository marketDataRepository;
    private final IntegrationLogRepository integrationLogRepository;
    private final AuditService auditService;
    private final Clock clock;

    public ReportService(AlertRepository alertRepository, MarketDataRepository marketDataRepository,
            IntegrationLogRepository integrationLogRepository, AuditService auditService, Clock clock) {
        this.alertRepository = alertRepository;
        this.marketDataRepository = marketDataRepository;
        this.integrationLogRepository = integrationLogRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    public StatisticsResponse statistics(String range, Integer userId) {
        int dayCount = switch (range == null || range.isBlank() ? "1d" : range) {
            case "1d" -> 1;
            case "5d" -> 5;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "El rango debe ser 1d o 5d.");
        };
        Instant now = clock.instant();
        List<LocalDate> days = ReportCalculator.tradingDays(now, dayCount);
        LocalDateTime from = DateTimes.toUtc(days.getFirst().atStartOfDay(DateTimes.NEW_YORK));
        LocalDateTime to = DateTimes.toUtc(days.getLast().plusDays(1).atStartOfDay(DateTimes.NEW_YORK));

        List<Alert> alerts = alertRepository.findByEmittedAtBetweenOrderByEmittedAtAsc(from, to);
        List<MarketData> data = marketDataRepository.findByWindowEndBetweenOrderByWindowEndAsc(from, to);
        List<IntegrationLog> failures = integrationLogRepository
                .findByServiceAndCreatedAtBetweenOrderByCreatedAtAsc(IntegrationLogService.YAHOO_FINANCE, from, to);

        long buy = count(alerts, Recommendation.BUY);
        long sell = count(alerts, Recommendation.SELL);
        long hold = count(alerts, Recommendation.HOLD);
        long actionableWithWarning = alerts.stream()
                .filter(a -> a.getRecommendation() != Recommendation.HOLD)
                .filter(a -> volatility(a).isHigh())
                .count();

        int expected = ReportCalculator.expectedCycles(days, now);
        StatisticsResponse.DataAvailability availability = new StatisticsResponse.DataAvailability(data.size(),
                expected, ReportCalculator.percent(Math.min(data.size(), expected), expected));

        StatisticsResponse.CyclesWithoutRecommendation withoutRecommendation =
                new StatisticsResponse.CyclesWithoutRecommendation(failures.size(), failures.stream()
                        .map(f -> new StatisticsResponse.FailedCycle(DateTimes.toBogota(f.getCreatedAt()),
                                f.getStatus(), f.getErrorMessage()))
                        .toList());

        Map<LocalDate, List<Alert>> byDay = alerts.stream().collect(Collectors.groupingBy(
                a -> DateTimes.toNewYork(a.getEmittedAt()).toLocalDate()));
        List<StatisticsResponse.DailySignals> perDay = days.stream()
                .map(day -> {
                    List<Alert> dayAlerts = byDay.getOrDefault(day, List.of());
                    return new StatisticsResponse.DailySignals(day, count(dayAlerts, Recommendation.BUY),
                            count(dayAlerts, Recommendation.SELL), count(dayAlerts, Recommendation.HOLD));
                })
                .toList();

        auditService.record(userId, AuditAction.REPORT_VIEWED, "Estadísticas del periodo " + range);
        return new StatisticsResponse(dayCount == 1 ? "1d" : "5d", days.getFirst(), days.getLast(),
                new StatisticsResponse.Counts(buy, sell, hold),
                ReportCalculator.percent(actionableWithWarning, buy + sell), withoutRecommendation, availability,
                performance(alerts, data), perDay);
    }

    public List<CycleHistoryItem> history(String types, LocalDate from, LocalDate to, Integer userId) {
        Filters.checkRange(from, to);
        boolean filtered = types != null && !types.isBlank();
        List<Recommendation> wanted = Filters.types(types, ALL_TYPES);
        LocalDateTime start = Filters.fromDay(from);
        LocalDateTime end = Filters.toDay(to);

        List<MarketData> data = marketDataRepository.findByWindowEndBetweenOrderByWindowEndAsc(start, end);
        Map<Integer, Alert> alertsByData = data.isEmpty() ? Map.of()
                : alertRepository.findByMarketDataIdIn(data.stream().map(MarketData::getId).toList()).stream()
                        .collect(Collectors.toMap(Alert::getMarketDataId, Function.identity(), (a, b) -> b));

        List<CycleHistoryItem> items = new ArrayList<>();
        for (MarketData d : data) {
            Alert alert = alertsByData.get(d.getId());
            if (alert == null || !wanted.contains(alert.getRecommendation())) {
                continue;
            }
            items.add(new CycleHistoryItem(DateTimes.toBogota(d.getWindowEnd()), CycleHistoryItem.RECOMMENDATION,
                    alert.getId(), alert.getRecommendation(), alert.getIndicatorValue(), d.getIndexValue(),
                    alert.getDetectedCondition(), null));
        }
        if (!filtered) {
            for (IntegrationLog log : integrationLogRepository.findByServiceAndCreatedAtBetweenOrderByCreatedAtAsc(
                    IntegrationLogService.YAHOO_FINANCE, start, end)) {
                String status = IntegrationLogService.STATUS_INSUFFICIENT_DATA.equals(log.getStatus())
                        ? CycleHistoryItem.INSUFFICIENT_DATA : CycleHistoryItem.ERROR;
                items.add(new CycleHistoryItem(DateTimes.toBogota(log.getCreatedAt()), status, null, null, null,
                        null, null, log.getErrorMessage()));
            }
        }
        items.sort(Comparator.comparing(CycleHistoryItem::time, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed());
        auditService.record(userId, AuditAction.REPORT_VIEWED, "Historial completo de señales");
        return items;
    }

    private StatisticsResponse.TheoreticalPerformance performance(List<Alert> alerts, List<MarketData> data) {
        if (data.isEmpty()) {
            return new StatisticsResponse.TheoreticalPerformance(null, null, null, List.of());
        }
        Map<Integer, MarketData> dataById = data.stream()
                .collect(Collectors.toMap(MarketData::getId, Function.identity()));
        List<ReportCalculator.Signal> signals = alerts.stream()
                .filter(a -> dataById.containsKey(a.getMarketDataId()))
                .map(a -> new ReportCalculator.Signal(DateTimes.toInstant(a.getEmittedAt()), a.getRecommendation(),
                        dataById.get(a.getMarketDataId()).getIndexValue()))
                .toList();
        MarketData first = data.getFirst();
        MarketData last = data.getLast();
        ReportCalculator.Performance performance = ReportCalculator.performance(signals, first.getIndexValue(),
                DateTimes.toInstant(last.getWindowEnd()), last.getIndexValue());
        BigDecimal difference = performance.buyAndHoldReturnPercent() == null ? null
                : performance.strategyReturnPercent().subtract(performance.buyAndHoldReturnPercent());
        return new StatisticsResponse.TheoreticalPerformance(performance.strategyReturnPercent(),
                performance.buyAndHoldReturnPercent(), difference, performance.trades().stream()
                        .map(t -> new StatisticsResponse.Trade(DateTimes.toBogota(t.buyTime()), t.buyPrice(),
                                DateTimes.toBogota(t.sellTime()), t.sellPrice(), t.returnPercent(), t.open()))
                        .toList());
    }

    private static long count(List<Alert> alerts, Recommendation type) {
        return alerts.stream().filter(a -> a.getRecommendation() == type).count();
    }

    private static Volatility volatility(Alert alert) {
        return Volatility.of(DateTimes.toNewYork(alert.getEmittedAt()).toZonedDateTime());
    }
}
