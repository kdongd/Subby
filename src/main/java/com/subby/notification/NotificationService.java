package com.subby.notification;

import com.subby.exchange.ExchangeRateService;
import com.subby.exchange.RateTable;
import com.subby.global.config.AppProperties;
import com.subby.mail.MailSender;
import com.subby.mail.MailTemplates;
import com.subby.stats.Payment;
import com.subby.stats.SubscriptionAmounts;
import com.subby.subscription.Subscription;
import com.subby.subscription.SubscriptionRepository;
import com.subby.subscription.SubscriptionStatus;
import com.subby.user.User;
import com.subby.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 결제 예정 알림. 하루 한 번 예약 작업이 호출한다.
 *
 * <p>사용자가 고른 시점(1·3·7일 전) 안에 들어온 결제 중 아직 알리지 않은 것을 모아 메일 1통으로 보낸다.
 * "정확히 N일 전"이 아니라 "N일 이내 + 미발송"으로 찾기 때문에, 예약 작업이 하루 빠져도 다음 날 알림이 간다.
 * 발송 기록을 먼저 저장하고 메일이 실패하면 기록을 지워서, 다음 실행에서 다시 시도한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final ExchangeRateService exchangeRateService;
    private final MailSender mailSender;
    private final MailTemplates mailTemplates;
    private final UnsubscribeTokenService unsubscribeTokenService;
    private final TransactionTemplate transactionTemplate;
    private final AppProperties properties;
    private final Clock clock;

    public Result sendDueNotifications() {
        LocalDate today = LocalDate.now(clock);
        RateTable rates = exchangeRateService.currentTable();
        int mails = 0;
        int payments = 0;
        int failures = 0;
        for (User user : userRepository.findAllByNotifyEnabledTrueAndEmailIsNotNull()) {
            try {
                int sent = notifyUser(user, today, rates);
                if (sent > 0) {
                    mails++;
                    payments += sent;
                }
            } catch (RuntimeException e) {
                failures++;
                log.warn("결제 알림 실패 userId={}", user.getId(), e);
            }
        }
        return new Result(mails, payments, failures);
    }

    private int notifyUser(User user, LocalDate today, RateTable rates) {
        List<Subscription> active = subscriptionRepository.findAllByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE);
        List<DuePayment> due = Payment.between(active, today, today.plusDays(user.getNotifyDaysBefore()), today, rates)
                .stream()
                .filter(p -> !notificationLogRepository.existsBySubscriptionIdAndPaymentDate(
                        p.subscription().getId(), p.date()))
                .map(NotificationService::toDuePayment)
                .toList();
        if (due.isEmpty()) {
            return 0;
        }

        Instant now = clock.instant();
        List<Long> logIds = transactionTemplate.execute(status -> due.stream()
                .map(p -> notificationLogRepository.save(new NotificationLog(p.subscriptionId(), p.paymentDate(), now)))
                .map(NotificationLog::getId)
                .toList());

        String unsubscribeLink = properties.baseUrl() + "/unsubscribe?token=" + unsubscribeTokenService.create(user.getId());
        try {
            mailSender.send(user.getEmail(),
                    mailTemplates.paymentReminderSubject(due.size()),
                    mailTemplates.paymentReminder(user.getNickname(), due, unsubscribeLink));
        } catch (RuntimeException e) {
            transactionTemplate.executeWithoutResult(status -> notificationLogRepository.deleteAllById(logIds));
            throw e;
        }
        return due.size();
    }

    private static DuePayment toDuePayment(Payment payment) {
        Subscription subscription = payment.subscription();
        return new DuePayment(subscription.getId(), subscription.getName(), payment.date(),
                subscription.getCurrency(), SubscriptionAmounts.display(subscription), payment.amountKrw());
    }

    /**
     * @param mails    보낸 메일 수(사용자 수)
     * @param payments 알린 결제 건수
     * @param failures 실패한 사용자 수
     */
    public record Result(int mails, int payments, int failures) {
    }
}
