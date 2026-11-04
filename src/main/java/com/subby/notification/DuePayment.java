package com.subby.notification;

import com.subby.subscription.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 알림으로 알려 줄 결제 1건 */
public record DuePayment(
        Long subscriptionId,
        String name,
        LocalDate paymentDate,
        CurrencyCode currency,
        BigDecimal amount,
        long amountKrw
) {
}
