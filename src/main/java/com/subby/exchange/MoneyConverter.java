package com.subby.exchange;

import com.subby.subscription.CurrencyCode;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyConverter {

    private MoneyConverter() {
    }

    /** 원화 금액(원 단위 반올림). 달러는 주어진 환율로 환산한다. */
    public static long toKrw(BigDecimal amount, CurrencyCode currency, BigDecimal usdKrwRate) {
        BigDecimal krw = currency == CurrencyCode.KRW ? amount : amount.multiply(usdKrwRate);
        return krw.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
