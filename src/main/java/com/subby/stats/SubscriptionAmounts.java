package com.subby.stats;

import com.subby.subscription.Subscription;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SubscriptionAmounts {

    private SubscriptionAmounts() {
    }

    /** 화면 표시용 금액. 원은 소수점 없이(12000), 달러는 소수 둘째 자리까지(20.00) */
    public static BigDecimal display(Subscription subscription) {
        return subscription.getAmount().setScale(subscription.getCurrency().scale(), RoundingMode.HALF_UP);
    }
}
