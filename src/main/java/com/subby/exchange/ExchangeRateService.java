package com.subby.exchange;

import com.subby.global.config.AppProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    static final String USD = "USD";
    static final String KRW = "KRW";

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateClient exchangeRateClient;
    private final AppProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RateTable currentTable() {
        Map<LocalDate, BigDecimal> rates = exchangeRateRepository.findAllByBaseAndQuote(USD, KRW).stream()
                .collect(Collectors.toMap(ExchangeRate::getRateDate, ExchangeRate::getRate));
        return new RateTable(rates, properties.exchange().fallbackUsdKrw());
    }

    /**
     * 마지막으로 저장한 날짜 다음 날부터 오늘까지의 환율을 받아 저장한다.
     * 처음 실행이면 과거 통계에도 환율을 쓸 수 있도록 initial-months개월 전부터 받는다.
     *
     * @return 새로 저장한 날짜 수
     */
    public int fetchAndStore() {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = exchangeRateRepository.findTopByBaseAndQuoteOrderByRateDateDesc(USD, KRW)
                .map(latest -> latest.getRateDate().plusDays(1))
                .orElse(today.minusMonths(properties.exchange().initialMonths()));
        if (from.isAfter(today)) {
            return 0;
        }
        // 외부 API 호출은 DB 트랜잭션 밖에서 한다. 응답을 기다리는 동안 DB 연결을 붙잡지 않기 위해서다.
        Map<LocalDate, BigDecimal> rates = exchangeRateClient.fetchUsdKrw(from, today);
        exchangeRateRepository.saveAll(rates.entrySet().stream()
                .map(entry -> new ExchangeRate(USD, KRW, entry.getKey(), entry.getValue()))
                .toList());
        log.info("환율 {}건 저장 ({} ~ {})", rates.size(), from, today);
        return rates.size();
    }
}
