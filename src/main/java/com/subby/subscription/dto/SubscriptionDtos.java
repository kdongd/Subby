package com.subby.subscription.dto;

import com.subby.exchange.MoneyConverter;
import com.subby.exchange.RateTable;
import com.subby.stats.SubscriptionAmounts;
import com.subby.subscription.BillingCycle;
import com.subby.subscription.Category;
import com.subby.subscription.CurrencyCode;
import com.subby.subscription.Subscription;
import com.subby.subscription.SubscriptionDetails;
import com.subby.subscription.SubscriptionStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SubscriptionDtos {

    private SubscriptionDtos() {
    }

    /** 통화별 금액 범위와 소수 자릿수는 SubscriptionService에서 추가로 검사한다. */
    public record SubscriptionRequest(
            @NotBlank(message = "서비스명을 입력해 주세요.")
            @Size(max = 50, message = "서비스명은 50자 이하로 입력해 주세요.")
            String name,

            @NotNull(message = "카테고리를 선택해 주세요.")
            Category category,

            @NotNull(message = "통화를 선택해 주세요.")
            CurrencyCode currency,

            @NotNull(message = "결제 금액을 입력해 주세요.")
            @Digits(integer = 8, fraction = 2, message = "금액 형식이 올바르지 않습니다.")
            BigDecimal amount,

            @NotNull(message = "결제 주기를 선택해 주세요.")
            BillingCycle billingCycle,

            @NotNull(message = "첫 결제일을 입력해 주세요.")
            LocalDate startDate,

            @Size(max = 200, message = "메모는 200자 이하로 입력해 주세요.")
            String memo
    ) {

        public SubscriptionDetails toDetails() {
            String trimmedMemo = memo == null || memo.isBlank() ? null : memo.trim();
            return new SubscriptionDetails(name.trim(), category, currency, amount, billingCycle, startDate, trimmedMemo);
        }
    }

    public record CancelRequest(
            @NotNull(message = "해지일을 입력해 주세요.") LocalDate canceledAt
    ) {
    }

    /**
     * @param amountKrw         결제 1회 금액의 원화 환산액 (달러는 최신 환율 기준 추정치)
     * @param nextPaymentDate   다음 결제일. 해지돼서 더 이상 결제가 없으면 null
     * @param upcomingPayments  오늘 이후 결제일 최대 3개
     */
    public record SubscriptionResponse(
            Long id,
            String name,
            Category category,
            CurrencyCode currency,
            BigDecimal amount,
            long amountKrw,
            BillingCycle billingCycle,
            LocalDate startDate,
            SubscriptionStatus status,
            LocalDate canceledAt,
            String memo,
            LocalDate nextPaymentDate,
            List<LocalDate> upcomingPayments
    ) {

        public static SubscriptionResponse of(Subscription subscription, LocalDate today, RateTable rates) {
            return new SubscriptionResponse(
                    subscription.getId(),
                    subscription.getName(),
                    subscription.getCategory(),
                    subscription.getCurrency(),
                    SubscriptionAmounts.display(subscription),
                    MoneyConverter.toKrw(subscription.getAmount(), subscription.getCurrency(), rates.latestRate()),
                    subscription.getBillingCycle(),
                    subscription.getStartDate(),
                    subscription.getStatus(),
                    subscription.getCanceledAt(),
                    subscription.getMemo(),
                    subscription.nextPaymentDate(today).orElse(null),
                    subscription.upcomingPaymentDates(today, 3));
        }
    }
}
