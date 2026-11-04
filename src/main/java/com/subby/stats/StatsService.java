package com.subby.stats;

import com.subby.exchange.ExchangeRateService;
import com.subby.exchange.MoneyConverter;
import com.subby.exchange.RateTable;
import com.subby.stats.dto.StatsDtos.CategoryAmount;
import com.subby.stats.dto.StatsDtos.CategoryStatsResponse;
import com.subby.stats.dto.StatsDtos.DashboardResponse;
import com.subby.stats.dto.StatsDtos.ExchangeRateInfo;
import com.subby.stats.dto.StatsDtos.MonthlyAmount;
import com.subby.stats.dto.StatsDtos.UpcomingPayment;
import com.subby.subscription.Category;
import com.subby.subscription.Subscription;
import com.subby.subscription.SubscriptionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지출 집계.
 * <ul>
 *   <li>실제 결제액: 그 기간에 실제 결제일이 있는 구독만 더한다. 연간 구독은 결제되는 달에만 전액 잡힌다.</li>
 *   <li>월 평균 환산액: 매월은 그대로, 매년은 ÷12, 매주는 ×52÷12. 활성 구독만 대상이다.</li>
 * </ul>
 * 달러는 지난 결제는 결제일 환율, 앞으로의 결제는 최신 환율로 원화 환산한 뒤 더한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    static final int UPCOMING_DAYS = 7;
    static final int MAX_MONTHS = 24;

    private final SubscriptionRepository subscriptionRepository;
    private final ExchangeRateService exchangeRateService;
    private final Clock clock;

    public DashboardResponse dashboard(Long userId) {
        LocalDate today = LocalDate.now(clock);
        YearMonth thisMonth = YearMonth.from(today);
        List<Subscription> subscriptions = subscriptionRepository.findAllByUserId(userId);
        List<Subscription> active = subscriptions.stream().filter(Subscription::isActive).toList();
        RateTable rates = exchangeRateService.currentTable();

        List<Payment> monthPayments = Payment.between(subscriptions,
                thisMonth.atDay(1), thisMonth.atEndOfMonth(), today, rates);
        long total = Payment.sum(monthPayments);
        long remaining = Payment.sum(monthPayments.stream().filter(p -> !p.date().isBefore(today)).toList());

        List<UpcomingPayment> upcoming = Payment.between(active, today, today.plusDays(UPCOMING_DAYS), today, rates)
                .stream()
                .map(payment -> toUpcoming(payment, today))
                .toList();

        return new DashboardResponse(
                thisMonth.toString(),
                active.size(),
                total,
                remaining,
                monthlyAverage(active, rates),
                upcoming,
                byCategory(monthPayments, total),
                new ExchangeRateInfo(rates.latestRate(), rates.latestDate(), rates.usingFallback()));
    }

    /** 이번 달을 포함한 최근 months개월의 월별 실제 결제액 (오래된 달부터) */
    public List<MonthlyAmount> monthly(Long userId, int months) {
        int count = Math.max(1, Math.min(months, MAX_MONTHS));
        LocalDate today = LocalDate.now(clock);
        YearMonth thisMonth = YearMonth.from(today);
        List<Subscription> subscriptions = subscriptionRepository.findAllByUserId(userId);
        RateTable rates = exchangeRateService.currentTable();

        List<MonthlyAmount> result = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            YearMonth month = thisMonth.minusMonths(i);
            List<Payment> payments = Payment.between(subscriptions, month.atDay(1), month.atEndOfMonth(), today, rates);
            result.add(new MonthlyAmount(month.toString(), Payment.sum(payments)));
        }
        return result;
    }

    public CategoryStatsResponse categories(Long userId, YearMonth month) {
        LocalDate today = LocalDate.now(clock);
        YearMonth target = month != null ? month : YearMonth.from(today);
        List<Subscription> subscriptions = subscriptionRepository.findAllByUserId(userId);
        RateTable rates = exchangeRateService.currentTable();
        List<Payment> payments = Payment.between(subscriptions, target.atDay(1), target.atEndOfMonth(), today, rates);
        long total = Payment.sum(payments);
        return new CategoryStatsResponse(target.toString(), total, byCategory(payments, total));
    }

    static long monthlyAverage(List<Subscription> active, RateTable rates) {
        long sum = 0;
        for (Subscription subscription : active) {
            BigDecimal krw = BigDecimal.valueOf(MoneyConverter.toKrw(subscription.getAmount(),
                    subscription.getCurrency(), rates.latestRate()));
            BigDecimal monthly = switch (subscription.getBillingCycle()) {
                case MONTHLY -> krw;
                case YEARLY -> krw.divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
                case WEEKLY -> krw.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
            };
            sum += monthly.longValueExact();
        }
        return sum;
    }

    private static List<CategoryAmount> byCategory(List<Payment> payments, long total) {
        Map<Category, long[]> sums = new EnumMap<>(Category.class);
        for (Payment payment : payments) {
            long[] values = sums.computeIfAbsent(payment.subscription().getCategory(), c -> new long[2]);
            values[0] += payment.amountKrw();
            values[1]++;
        }
        return sums.entrySet().stream()
                .map(entry -> new CategoryAmount(entry.getKey(), entry.getValue()[0], (int) entry.getValue()[1],
                        total == 0 ? 0 : Math.round(entry.getValue()[0] * 10000.0 / total) / 10000.0))
                .sorted(Comparator.comparingLong(CategoryAmount::amountKrw).reversed())
                .toList();
    }

    private static UpcomingPayment toUpcoming(Payment payment, LocalDate today) {
        Subscription subscription = payment.subscription();
        return new UpcomingPayment(
                subscription.getId(),
                subscription.getName(),
                subscription.getCategory(),
                payment.date(),
                ChronoUnit.DAYS.between(today, payment.date()),
                subscription.getCurrency(),
                SubscriptionAmounts.display(subscription),
                payment.amountKrw());
    }
}
