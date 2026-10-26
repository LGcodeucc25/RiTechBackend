package com.ritech.backend.market;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/summary")
    public MarketSummaryResponse summary() {
        return marketService.summary();
    }

    @GetMapping("/history")
    public List<ChartPoint> history(@RequestParam(defaultValue = "1d") String range) {
        return marketService.history(range);
    }
}
