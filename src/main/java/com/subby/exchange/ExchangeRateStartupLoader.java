package com.subby.exchange;

import com.subby.global.config.AppProperties;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 환율이 하나도 없으면 서버 시작 직후 백그라운드로 한 번 받아온다.
 * 첫 배포 직후 예약 작업이 돌기 전까지 기본 환율로 계산되는 시간을 줄이기 위해서다. 실패해도 서버 시작에는 영향이 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateStartupLoader {

    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateRepository exchangeRateRepository;
    private final AppProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void loadIfEmpty() {
        if (!properties.exchange().fetchOnStartup() || exchangeRateRepository.count() > 0) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                exchangeRateService.fetchAndStore();
            } catch (RuntimeException e) {
                log.warn("시작 시 환율 수집 실패. 예약 작업에서 다시 시도합니다.", e);
            }
        });
    }
}
