package com.subby.stats;

import com.subby.global.security.UserPrincipal;
import com.subby.stats.dto.StatsDtos.CategoryStatsResponse;
import com.subby.stats.dto.StatsDtos.DashboardResponse;
import com.subby.stats.dto.StatsDtos.MonthlyAmount;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return statsService.dashboard(principal.getId());
    }

    @GetMapping("/stats/monthly")
    public List<MonthlyAmount> monthly(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestParam(defaultValue = "6") int months) {
        return statsService.monthly(principal.getId(), months);
    }

    @GetMapping("/stats/categories")
    public CategoryStatsResponse categories(@AuthenticationPrincipal UserPrincipal principal,
                                            @RequestParam(required = false) YearMonth yearMonth) {
        return statsService.categories(principal.getId(), yearMonth);
    }
}
