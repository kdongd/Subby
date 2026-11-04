package com.subby.notification;

import com.subby.exchange.ExchangeRateService;
import com.subby.exchange.MoneyFormat;
import com.subby.exchange.RateTable;
import com.subby.push.PushMessage;
import com.subby.push.PushService;
import com.subby.stats.Payment;
import com.subby.stats.SubscriptionAmounts;
import com.subby.subscription.Subscription;
import com.subby.subscription.SubscriptionRepository;
import com.subby.subscription.SubscriptionStatus;
import com.subby.user.User;
import com.subby.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 결제 예정 알림. 하루 한 번 예약 작업이 호출한다.
 *
 * <p>사용자가 고른 시점(1·3·7일 전) 안에 들어온 결제 중 아직 알리지 않은 것마다 사이트 안 알림(🔔)을 만들고,
 * 그 사용자의 기기마다 요약 푸시를 1개씩 보낸다.
 * "정확히 N일 전"이 아니라 "N일 이내 + 아직 알림 없음"으로 찾기 때문에, 예약 작업이 하루 빠져도 다음 날 알림이 간다.
 *
 * <p>푸시는 보조 수단이다. 기기가 꺼져 있거나 푸시가 실패해도 사이트 안 알림은 남고, 같은 결제로 푸시를 다시 보내지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** 이보다 오래된 사이트 안 알림은 지운다. 알림 대상 결제는 최대 7일 앞이라 지워도 다시 생기지 않는다. */
    static final Duration RETENTION = Duration.ofDays(90);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final ExchangeRateService exchangeRateService;
    private final PushService pushService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public Result sendDueNotifications() {
        LocalDate today = LocalDate.now(clock);
        RateTable rates = exchangeRateService.currentTable();
        int users = 0;
        int created = 0;
        int pushes = 0;
        int failures = 0;
        for (User user : userRepository.findAllByNotifyEnabledTrue()) {
            try {
                List<Notification> notifications = createNotifications(user, today, rates);
                if (notifications.isEmpty()) {
                    continue;
                }
                users++;
                created += notifications.size();
                pushes += pushService.sendToUser(user.getId(), summary(notifications, today));
            } catch (RuntimeException e) {
                failures++;
                log.warn("결제 알림 실패 userId={}", user.getId(), e);
            }
        }
        int deleted = deleteOld();
        return new Result(users, created, pushes, failures, deleted);
    }

    private List<Notification> createNotifications(User user, LocalDate today, RateTable rates) {
        List<Subscription> active = subscriptionRepository.findAllByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE);
        List<DuePayment> due = Payment.between(active, today, today.plusDays(user.getNotifyDaysBefore()), today, rates)
                .stream()
                .filter(p -> !notificationRepository.existsBySubscriptionIdAndPaymentDate(
                        p.subscription().getId(), p.date()))
                .map(NotificationService::toDuePayment)
                .toList();
        if (due.isEmpty()) {
            return List.of();
        }
        Instant now = clock.instant();
        try {
            return transactionTemplate.execute(status -> notificationRepository.saveAllAndFlush(due.stream()
                    .map(p -> new Notification(user.getId(), p.subscriptionId(), p.paymentDate(),
                            p.name() + " 결제 예정", body(p), "/subscriptions/" + p.subscriptionId(), now))
                    .toList()));
        } catch (DataIntegrityViolationException e) {
            // 예약 작업이 동시에 두 번 실행된 경우. 다른 쪽이 이미 만들었으니 이번에는 넘어간다.
            log.info("이미 만들어진 결제 알림이 있어 건너뜀 userId={}", user.getId());
            return List.of();
        }
    }

    /**
     * 사이트 안 알림 본문. 예: "11월 5일 (목) · $20.00 (약 27,600원)"
     * "내일" 같은 상대 표현은 나중에 읽으면 틀리므로 저장하지 않고, 화면과 푸시에서 붙인다.
     */
    static String body(DuePayment payment) {
        return DATE.format(payment.paymentDate()) + " · "
                + MoneyFormat.withKrw(payment.currency(), payment.amount(), payment.amountKrw());
    }

    static String relativeDay(LocalDate date, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, date);
        if (days <= 0) {
            return "오늘";
        }
        if (days == 1) {
            return "내일";
        }
        return days + "일 후";
    }

    /** 기기에는 한 번에 하나만 띄운다. 여러 건이면 묶어서 보여 주고 알림 목록으로 보낸다. */
    static PushMessage summary(List<Notification> notifications, LocalDate today) {
        String tag = "subby-due-" + today;
        if (notifications.size() == 1) {
            Notification only = notifications.get(0);
            return new PushMessage(only.getTitle(), relativeDay(only.getPaymentDate(), today) + " · " + only.getBody(),
                    only.getLink(), tag);
        }
        String names = notifications.stream()
                .limit(3)
                .map(n -> n.getTitle().replace(" 결제 예정", ""))
                .collect(Collectors.joining(", "));
        int rest = notifications.size() - 3;
        String body = rest > 0 ? names + " 외 " + rest + "건" : names;
        return new PushMessage("곧 결제될 구독 " + notifications.size() + "건", body, "/notifications", tag);
    }

    private int deleteOld() {
        Instant before = clock.instant().minus(RETENTION);
        Integer deleted = transactionTemplate.execute(status -> notificationRepository.deleteCreatedBefore(before));
        return deleted == null ? 0 : deleted;
    }

    private static DuePayment toDuePayment(Payment payment) {
        Subscription subscription = payment.subscription();
        return new DuePayment(subscription.getId(), subscription.getName(), payment.date(),
                subscription.getCurrency(), SubscriptionAmounts.display(subscription), payment.amountKrw());
    }

    /**
     * @param users         알림을 받은 사용자 수
     * @param notifications 새로 만든 사이트 안 알림 수(= 결제 건수)
     * @param pushesSent    푸시가 도착한 기기 수
     * @param failures      실패한 사용자 수
     * @param deletedOld    보관 기간이 지나 지운 알림 수
     */
    public record Result(int users, int notifications, int pushesSent, int failures, int deletedOld) {
    }
}
