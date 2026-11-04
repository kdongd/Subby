package com.subby.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.subby.mail.MailSendException;
import com.subby.subscription.BillingCycle;
import com.subby.subscription.Category;
import com.subby.subscription.CurrencyCode;
import com.subby.subscription.Subscription;
import com.subby.subscription.SubscriptionDetails;
import com.subby.subscription.SubscriptionRepository;
import com.subby.support.IntegrationTestSupport;
import com.subby.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class NotificationIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private UnsubscribeTokenService unsubscribeTokenService;

    private void subscribe(User user, String name, LocalDate startDate) {
        subscriptionRepository.save(Subscription.create(user.getId(), new SubscriptionDetails(
                name, Category.OTT, CurrencyCode.KRW, new BigDecimal("9900"), BillingCycle.MONTHLY, startDate, null)));
    }

    @Test
    @DisplayName("알림 시점 안에 결제가 있으면 메일 1통을 보내고, 같은 결제는 다시 알리지 않는다")
    void sendOnce() {
        User user = createUser("notify@example.com");
        subscribe(user, "넷플릭스", TODAY.plusDays(1));
        subscribe(user, "티빙", TODAY.plusDays(20));

        NotificationService.Result first = notificationService.sendDueNotifications();
        NotificationService.Result second = notificationService.sendDueNotifications();

        assertThat(first.mails()).isEqualTo(1);
        assertThat(first.payments()).isEqualTo(1);
        assertThat(second.mails()).isZero();
        verify(mailSender, times(1)).send(eq("notify@example.com"), anyString(), anyString());
        assertThat(notificationLogRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("메일 발송이 실패하면 발송 기록을 지워서 다음 실행에서 다시 시도한다")
    void retryAfterFailure() {
        User user = createUser("fail@example.com");
        subscribe(user, "웨이브", TODAY);
        doThrow(new MailSendException("down", null)).when(mailSender).send(anyString(), anyString(), anyString());

        NotificationService.Result result = notificationService.sendDueNotifications();

        assertThat(result.failures()).isEqualTo(1);
        assertThat(notificationLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("알림을 끈 사용자에게는 보내지 않는다")
    void disabled() {
        User user = createUser("off@example.com");
        user.updateNotification(false, 1);
        userRepository.save(user);
        subscribe(user, "쿠팡플레이", TODAY);

        notificationService.sendDueNotifications();

        verify(mailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("메일의 알림 끄기 토큰으로 로그인 없이 알림을 끌 수 있고, 위조한 토큰은 거부한다")
    void unsubscribe() throws Exception {
        User user = createUser("unsub@example.com");
        String token = unsubscribeTokenService.create(user.getId());

        mockMvc.perform(post("/api/notifications/unsubscribe").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", user.getId() + ".forged"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/notifications/unsubscribe").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token))))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId()).orElseThrow().isNotifyEnabled()).isFalse();
    }
}
