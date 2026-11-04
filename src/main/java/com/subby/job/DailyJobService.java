package com.subby.job;

import com.subby.exchange.ExchangeRateService;
import com.subby.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 하루 한 번 실행하는 작업: 환율 수집 → 결제 알림 발송.
 * 환율 수집이 실패해도 알림은 마지막으로 받은 환율로 계속 보낸다.
 * 같은 날 두 번 실행돼도 결과가 같다(환율은 덮어쓰고, 알림은 발송 기록 때문에 다시 가지 않는다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyJobService {

    private final ExchangeRateService exchangeRateService;
    private final NotificationService notificationService;

    public Result run() {
        int ratesSaved = 0;
        String rateError = null;
        try {
            ratesSaved = exchangeRateService.fetchAndStore();
        } catch (RuntimeException e) {
            rateError = e.getMessage();
            log.warn("환율 수집 실패", e);
        }
        NotificationService.Result notification = notificationService.sendDueNotifications();
        log.info("일일 작업 완료: 환율 {}건, 알림 메일 {}통({}건), 알림 실패 {}명",
                ratesSaved, notification.mails(), notification.payments(), notification.failures());
        return new Result(ratesSaved, rateError, notification.mails(), notification.payments(), notification.failures());
    }

    public record Result(int ratesSaved, String rateError, int mailsSent, int paymentsNotified, int failedUsers) {
    }
}
