package com.subby.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface ExchangeRateClient {

    /** from ~ to 기간의 날짜별 1달러당 원화 환율. 영업일이 아닌 날짜는 들어 있지 않다. */
    Map<LocalDate, BigDecimal> fetchUsdKrw(LocalDate from, LocalDate to);
}
