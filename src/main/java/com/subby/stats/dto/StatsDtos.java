package com.subby.stats.dto;

import com.subby.subscription.Category;
import com.subby.subscription.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class StatsDtos {

    private StatsDtos() {
    }

    public record DashboardResponse(
            String yearMonth,
            int activeCount,
            long thisMonthTotal,
            long thisMonthRemaining,
            long monthlyAverage,
            List<UpcomingPayment> upcoming,
            List<CategoryAmount> categories,
            ExchangeRateInfo exchangeRate
    ) {
    }

    public record UpcomingPayment(
            Long subscriptionId,
            String name,
            Category category,
            LocalDate paymentDate,
            long dDay,
            CurrencyCode currency,
            BigDecimal amount,
            long amountKrw
    ) {
    }

    public record CategoryAmount(Category category, long amountKrw, int count, double ratio) {
    }

    public record CategoryStatsResponse(String yearMonth, long total, List<CategoryAmount> items) {
    }

    public record MonthlyAmount(String yearMonth, long totalKrw) {
    }

    /**
     * @param rateDate 환율 기준일. 아직 환율을 받지 못했으면 null이고 fallback이 true
     */
    public record ExchangeRateInfo(BigDecimal usdKrw, LocalDate rateDate, boolean fallback) {
    }
}
