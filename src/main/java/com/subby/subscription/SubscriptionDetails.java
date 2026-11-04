package com.subby.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 구독 등록·수정에서 사용자가 입력하는 값 묶음 */
public record SubscriptionDetails(
        String name,
        Category category,
        CurrencyCode currency,
        BigDecimal amount,
        BillingCycle billingCycle,
        LocalDate startDate,
        String memo
) {
}
