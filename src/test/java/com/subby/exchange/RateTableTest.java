package com.subby.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subby.subscription.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateTableTest {

    private final RateTable table = new RateTable(Map.of(
            LocalDate.of(2026, 10, 30), new BigDecimal("1380.00"),
            LocalDate.of(2026, 11, 2), new BigDecimal("1390.00")), new BigDecimal("1400"));

    @Test
    @DisplayName("환율이 없는 날(주말)은 직전 영업일 환율을 쓴다")
    void weekendUsesPreviousBusinessDay() {
        assertThat(table.rateOn(LocalDate.of(2026, 11, 1))).isEqualByComparingTo("1380.00");
    }

    @Test
    @DisplayName("지난 결제는 결제일 환율, 앞으로의 결제는 최신 환율")
    void pastAndFuturePayments() {
        LocalDate today = LocalDate.of(2026, 11, 3);
        assertThat(table.rateForPayment(LocalDate.of(2026, 10, 31), today)).isEqualByComparingTo("1380.00");
        assertThat(table.rateForPayment(LocalDate.of(2026, 12, 1), today)).isEqualByComparingTo("1390.00");
    }

    @Test
    @DisplayName("환율이 하나도 없으면 기본 환율을 쓴다")
    void fallback() {
        RateTable empty = new RateTable(Map.of(), new BigDecimal("1400"));
        assertThat(empty.usingFallback()).isTrue();
        assertThat(empty.rateOn(LocalDate.of(2026, 11, 3))).isEqualByComparingTo("1400");
        assertThat(empty.latestDate()).isNull();
    }

    @Test
    @DisplayName("원화 환산은 원 단위 반올림")
    void convert() {
        assertThat(MoneyConverter.toKrw(new BigDecimal("20.00"), CurrencyCode.USD, new BigDecimal("1385.27")))
                .isEqualTo(27705L);
        assertThat(MoneyConverter.toKrw(new BigDecimal("17000"), CurrencyCode.KRW, new BigDecimal("1385.27")))
                .isEqualTo(17000L);
    }

    @Test
    @DisplayName("Frankfurter 기간 조회 응답을 날짜별 환율로 바꾼다")
    void parseFrankfurterResponse() {
        String body = """
                {"amount":1.0,"base":"USD","start_date":"2026-10-30","end_date":"2026-11-02",
                 "rates":{"2026-10-30":{"KRW":1380.5},"2026-11-02":{"KRW":1390.25}}}
                """;
        Map<LocalDate, BigDecimal> rates = FrankfurterClient.parse(new ObjectMapper(), body);
        assertThat(rates).hasSize(2);
        assertThat(rates.get(LocalDate.of(2026, 11, 2))).isEqualByComparingTo("1390.25");
    }
}
