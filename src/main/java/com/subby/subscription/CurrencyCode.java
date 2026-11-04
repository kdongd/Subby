package com.subby.subscription;

import java.math.BigDecimal;

/**
 * 결제 통화. java.util.Currency와 이름이 겹치지 않게 CurrencyCode로 둔다.
 */
public enum CurrencyCode {
    KRW(0, new BigDecimal("1"), new BigDecimal("10000000")),
    USD(2, new BigDecimal("0.01"), new BigDecimal("100000"));

    private final int scale;
    private final BigDecimal min;
    private final BigDecimal max;

    CurrencyCode(int scale, BigDecimal min, BigDecimal max) {
        this.scale = scale;
        this.min = min;
        this.max = max;
    }

    /** 원은 정수, 달러는 소수 둘째 자리까지 */
    public int scale() {
        return scale;
    }

    public BigDecimal min() {
        return min;
    }

    public BigDecimal max() {
        return max;
    }
}
