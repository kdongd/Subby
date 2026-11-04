package com.subby.notification;

import com.subby.subscription.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 알림 메일에 들어갈 결제 1건 */
public record DuePayment(
        Long subscriptionId,
        String name,
        LocalDate paymentDate,
        CurrencyCode currency,
        BigDecimal amount,
        long amountKrw
) {
}
