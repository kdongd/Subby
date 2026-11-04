package com.subby.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 달러→원 환율표. 한 요청 안에서 여러 결제일의 환율을 조회할 때 DB를 반복해서 부르지 않도록 메모리에 올려둔다.
 * 환율은 유럽중앙은행 영업일에만 있으므로, 해당 날짜에 없으면 직전 날짜 환율을 쓴다.
 */
public final class RateTable {

    private final NavigableMap<LocalDate, BigDecimal> rates;
    private final BigDecimal fallbackRate;

    public RateTable(Map<LocalDate, BigDecimal> rates, BigDecimal fallbackRate) {
        this.rates = new TreeMap<>(rates);
        this.fallbackRate = fallbackRate;
    }

    public BigDecimal rateOn(LocalDate date) {
        if (rates.isEmpty()) {
            return fallbackRate;
        }
        Map.Entry<LocalDate, BigDecimal> floor = rates.floorEntry(date);
        // 저장된 환율보다 이전 날짜면 가장 오래된 환율을 쓴다.
        return floor != null ? floor.getValue() : rates.firstEntry().getValue();
    }

    public BigDecimal latestRate() {
        return rates.isEmpty() ? fallbackRate : rates.lastEntry().getValue();
    }

    /** 저장된 환율이 없으면 null */
    public LocalDate latestDate() {
        return rates.isEmpty() ? null : rates.lastKey();
    }

    /** 환율을 아직 한 번도 받지 못해 기본값을 쓰고 있는지 */
    public boolean usingFallback() {
        return rates.isEmpty();
    }

    /**
     * 결제 1건에 쓸 환율. 이미 지난 결제(오늘 포함)는 그날 환율, 앞으로의 결제는 최신 환율이다.
     */
    public BigDecimal rateForPayment(LocalDate paymentDate, LocalDate today) {
        return paymentDate.isAfter(today) ? latestRate() : rateOn(paymentDate);
    }
}
