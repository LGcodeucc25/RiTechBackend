package com.ritech.backend.simulation;

import static com.ritech.backend.simulation.SimulationCalculator.money;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.ritech.backend.alert.AlertRepository;
import com.ritech.backend.alert.AlertSummary;
import com.ritech.backend.audit.AuditAction;
import com.ritech.backend.audit.AuditService;
import com.ritech.backend.common.ApiException;
import com.ritech.backend.common.DateTimes;

@Service
public class SimulationService {

    public static final String CURRENCY = "COP";
    static final String NOT_SAVED_WARNING =
            "No se pudo guardar la simulación. El resultado es válido, pero no quedará en tu historial.";
    static final String NOT_FOUND_MESSAGE = "No se encontró la simulación.";

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final SimulationRepository simulationRepository;
    private final AlertRepository alertRepository;
    private final AuditService auditService;
    private final Clock clock;

    public SimulationService(SimulationRepository simulationRepository, AlertRepository alertRepository,
            AuditService auditService, Clock clock) {
        this.simulationRepository = simulationRepository;
        this.alertRepository = alertRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    public SimulationResponse create(Integer userId, SimulationRequest request) {
        SimulationValidator.ValidInput input = SimulationValidator.validate(request);
        SimulationCalculator.Result result = SimulationCalculator.calculate(input.capital(), input.variation());
        LocalDateTime createdAt = DateTimes.nowUtc(clock);

        Simulation simulation = new Simulation(userId, money(input.capital()), input.variation(),
                money(result.resultPercent()), money(result.resultAmount()), money(result.finalValue()), createdAt);
        Integer id = null;
        boolean saved = false;
        if (fitsStorage(simulation)) {
            try {
                id = simulationRepository.save(simulation).getId();
                saved = true;
                auditService.record(userId, AuditAction.SIMULATION_CREATED, "Simulación " + id + " creada");
            } catch (DataAccessException ex) {
                log.error("Could not save simulation for user {}", userId, ex);
            }
        } else {
            log.warn("Simulation result for user {} exceeds numeric(12,2) and was not saved", userId);
        }

        return new SimulationResponse(id, money(input.capital()), money(input.variation()),
                money(result.rawPercent()), money(result.resultPercent()), money(result.resultAmount()),
                money(result.finalValue()), money(result.maxLoss()), result.capped(), CURRENCY,
                DateTimes.toBogota(createdAt), saved, saved ? null : NOT_SAVED_WARNING,
                recommendationAt(createdAt));
    }

    public List<SimulationResponse> list(Integer userId, String query, LocalDate from, LocalDate to) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return simulationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .filter(s -> inRange(s.getCreatedAt(), from, to))
                .filter(s -> needle.isEmpty() || matches(s, needle))
                .map(this::toResponse)
                .toList();
    }

    public SimulationResponse get(Integer userId, Integer id) {
        return simulationRepository.findByIdAndUserId(id, userId)
                .map(this::toResponse)
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND_MESSAGE));
    }

    private SimulationResponse toResponse(Simulation s) {
        SimulationCalculator.Result result = SimulationCalculator.calculate(s.getInitialCapital(),
                s.getNasdaqVariation());
        return new SimulationResponse(s.getId(), money(s.getInitialCapital()), money(s.getNasdaqVariation()),
                money(result.rawPercent()), money(s.getResultPercent()), money(s.getResultAmount()),
                money(s.getFinalValue()), money(s.getInitialCapital()), result.capped(), CURRENCY,
                DateTimes.toBogota(s.getCreatedAt()), true, null, recommendationAt(s.getCreatedAt()));
    }

    private AlertSummary recommendationAt(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return alertRepository.findFirstByEmittedAtLessThanEqualOrderByEmittedAtDescIdDesc(time)
                .map(AlertSummary::from)
                .orElse(null);
    }

    private static boolean fitsStorage(Simulation s) {
        return s.getResultAmount().abs().compareTo(SimulationValidator.MAX_CAPITAL) <= 0
                && s.getFinalValue().compareTo(SimulationValidator.MAX_CAPITAL) <= 0;
    }

    private static boolean inRange(LocalDateTime createdAtUtc, LocalDate from, LocalDate to) {
        if (createdAtUtc == null) {
            return from == null && to == null;
        }
        LocalDate day = DateTimes.toBogota(createdAtUtc).toLocalDate();
        return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
    }

    private static boolean matches(Simulation s, String needle) {
        return String.valueOf(s.getId()).equals(needle)
                || plain(s.getInitialCapital()).contains(needle)
                || plain(s.getNasdaqVariation()).contains(needle)
                || plain(s.getFinalValue()).contains(needle);
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
