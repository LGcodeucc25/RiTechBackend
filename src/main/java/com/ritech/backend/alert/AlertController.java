package com.ritech.backend.alert;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ritech.backend.auth.CurrentUser;
import com.ritech.backend.common.PageResponse;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public PageResponse<AlertListItem> list(@RequestParam(required = false) String types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return alertService.list(types, from, to, page, size);
    }

    @GetMapping("/{id}")
    public AlertDetail detail(@PathVariable Integer id, Authentication authentication) {
        return alertService.detail(id, CurrentUser.name(authentication));
    }
}
