package com.subby.job;

import com.subby.exchange.ExchangeRateService;
import com.subby.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 하루 한 번 실행하는 작업: 환율 수집 → 결제 알림 발송.
 * 환율 수집이 실패해도 알림은 마지막으로 받은 환율로 계속 보낸다.
 * 같은 날 두 번 실행돼도 결과가 같다(환율은 덮어쓰고, 알림은 결제 건마다 하나만 만들어진다).
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
        log.info("일일 작업 완료: 환율 {}건, 알림 {}명({}건), 푸시 {}대, 실패 {}명, 오래된 알림 삭제 {}건",
                ratesSaved, notification.users(), notification.notifications(), notification.pushesSent(),
                notification.failures(), notification.deletedOld());
        return new Result(ratesSaved, rateError, notification.users(), notification.notifications(),
                notification.pushesSent(), notification.failures());
    }

    public record Result(int ratesSaved, String rateError, int usersNotified, int paymentsNotified,
                         int pushesSent, int failedUsers) {
    }
}
