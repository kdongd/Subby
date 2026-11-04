package com.subby.exchange;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class ExchangeRateId implements Serializable {

    private String base;
    private String quote;
    private LocalDate rateDate;

    protected ExchangeRateId() {
    }

    public ExchangeRateId(String base, String quote, LocalDate rateDate) {
        this.base = base;
        this.quote = quote;
        this.rateDate = rateDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExchangeRateId other)) {
            return false;
        }
        return Objects.equals(base, other.base)
                && Objects.equals(quote, other.quote)
                && Objects.equals(rateDate, other.rateDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, quote, rateDate);
    }
}
