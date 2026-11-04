package com.subby.exchange;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 날짜별 환율. 1 base = rate quote (예: 1 USD = 1,380.25 KRW) */
@Entity
@Table(name = "exchange_rates")
@IdClass(ExchangeRateId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRate {

    @Id
    private String base;

    @Id
    private String quote;

    @Id
    @Column(name = "rate_date")
    private LocalDate rateDate;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal rate;

    public ExchangeRate(String base, String quote, LocalDate rateDate, BigDecimal rate) {
        this.base = base;
        this.quote = quote;
        this.rateDate = rateDate;
        this.rate = rate;
    }
}
