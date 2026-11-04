package com.subby.exchange;

import com.subby.subscription.CurrencyCode;
import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class MoneyFormat {

    private MoneyFormat() {
    }

    public static String krw(long amount) {
        return new DecimalFormat("#,##0").format(amount) + "원";
    }

    public static String usd(BigDecimal amount) {
        return "$" + new DecimalFormat("#,##0.00").format(amount);
    }

    /** 원화는 그대로, 달러는 "$20.00 (약 27,600원)" 형식 */
    public static String withKrw(CurrencyCode currency, BigDecimal amount, long amountKrw) {
        if (currency == CurrencyCode.KRW) {
            return krw(amountKrw);
        }
        return usd(amount) + " (약 " + krw(amountKrw) + ")";
    }
}
