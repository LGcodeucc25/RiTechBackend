package com.ritech.backend.report;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ritech.backend.auth.CurrentUser;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/statistics")
    public StatisticsResponse statistics(@RequestParam(defaultValue = "1d") String range,
            Authentication authentication) {
        return reportService.statistics(range, CurrentUser.id(authentication));
    }

    @GetMapping("/history")
    public List<CycleHistoryItem> history(@RequestParam(required = false) String types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return reportService.history(types, from, to, CurrentUser.id(authentication));
    }
}
