package com.subby.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.subby.push.PushSender.SendResult;
import com.subby.push.PushSubscription;
import com.subby.push.PushSubscriptionRepository;
import com.subby.subscription.BillingCycle;
import com.subby.subscription.Category;
import com.subby.subscription.CurrencyCode;
import com.subby.subscription.Subscription;
import com.subby.subscription.SubscriptionDetails;
import com.subby.subscription.SubscriptionRepository;
import com.subby.support.IntegrationTestSupport;
import com.subby.user.User;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private static final String P256DH = "BAvDis_3Rfel4GDVEBAvizXtKa12by0qtGT3UCYttSjmC79pkoyduVOlF8tDOR2W8N6Jl-kkggRsUQacIparIes";
    private static final String AUTH = "OSzrT-PR-1311pngWy9vzA";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;

    @BeforeEach
    void enablePush() {
        when(pushSender.isEnabled()).thenReturn(true);
        when(pushSender.send(any(), any())).thenReturn(SendResult.SENT);
    }

    private Subscription subscribe(User user, String name, LocalDate startDate) {
        return subscriptionRepository.save(Subscription.create(user.getId(), new SubscriptionDetails(
                name, Category.OTT, CurrencyCode.KRW, new BigDecimal("9900"), BillingCycle.MONTHLY, startDate, null)));
    }

    private PushSubscription device(User user, String id) {
        return pushSubscriptionRepository.save(new PushSubscription(user.getId(),
                "https://fcm.googleapis.com/fcm/send/" + id, P256DH, AUTH, "test", Instant.now()));
    }

    @Test
    @DisplayName("알림 시점 안의 결제마다 사이트 알림을 만들고, 기기마다 요약 푸시 1개를 보낸다. 다시 실행해도 중복되지 않는다")
    void createOnce() {
        User user = createUser("notify@example.com");
        subscribe(user, "넷플릭스", TODAY.plusDays(1));
        subscribe(user, "티빙", TODAY);
        subscribe(user, "웨이브", TODAY.plusDays(20));
        device(user, "phone");
        device(user, "laptop");

        NotificationService.Result first = notificationService.sendDueNotifications();
        NotificationService.Result second = notificationService.sendDueNotifications();

        assertThat(first.users()).isEqualTo(1);
        assertThat(first.notifications()).isEqualTo(2);
        assertThat(first.pushesSent()).isEqualTo(2);
        assertThat(second.notifications()).isZero();
        assertThat(notificationRepository.count()).isEqualTo(2);
        verify(pushSender, times(2)).send(any(), argThat(payload -> {
            String json = new String(payload, StandardCharsets.UTF_8);
            return json.contains("곧 결제될 구독 2건") && json.contains("/notifications");
        }));
    }

    @Test
    @DisplayName("결제가 1건이면 그 구독 화면으로 연결되는 푸시를 보낸다")
    void singlePayment() {
        User user = createUser("single@example.com");
        Subscription subscription = subscribe(user, "넷플릭스", TODAY.plusDays(1));
        device(user, "phone");

        notificationService.sendDueNotifications();

        verify(pushSender).send(any(), argThat(payload -> {
            String json = new String(payload, StandardCharsets.UTF_8);
            return json.contains("넷플릭스 결제 예정") && json.contains("내일")
                    && json.contains("/subscriptions/" + subscription.getId());
        }));
    }

    @Test
    @DisplayName("푸시 서버가 구독 만료(410)를 알려 주면 그 기기를 지운다. 사이트 알림은 남는다")
    void removesGoneDevice() {
        User user = createUser("gone@example.com");
        subscribe(user, "웨이브", TODAY);
        device(user, "old");
        when(pushSender.send(any(), any())).thenReturn(SendResult.GONE);

        NotificationService.Result result = notificationService.sendDueNotifications();

        assertThat(result.pushesSent()).isZero();
        assertThat(pushSubscriptionRepository.count()).isZero();
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("기기를 등록하지 않았어도 사이트 알림은 만든다")
    void withoutDevice() {
        User user = createUser("nodevice@example.com");
        subscribe(user, "디즈니+", TODAY);

        NotificationService.Result result = notificationService.sendDueNotifications();

        assertThat(result.notifications()).isEqualTo(1);
        verify(pushSender, never()).send(any(), any());
    }

    @Test
    @DisplayName("알림을 끈 사용자에게는 알림을 만들지 않는다")
    void disabled() {
        User user = createUser("off@example.com");
        user.updateNotification(false, 1);
        userRepository.save(user);
        subscribe(user, "쿠팡플레이", TODAY);
        device(user, "phone");

        notificationService.sendDueNotifications();

        assertThat(notificationRepository.count()).isZero();
        verify(pushSender, never()).send(any(), any());
    }

    @Test
    @DisplayName("알림 목록·안 읽은 수·읽음 처리. 다른 사람의 알림은 읽음 처리할 수 없다")
    void inbox() throws Exception {
        User me = createUser("me@example.com");
        User other = createUser("other@example.com");
        subscribe(me, "넷플릭스", TODAY);
        subscribe(me, "티빙", TODAY.plusDays(1));
        subscribe(other, "웨이브", TODAY);
        notificationService.sendDueNotifications();
        Long othersId = notificationRepository.findAll().stream()
                .filter(n -> n.getUserId().equals(other.getId())).findFirst().orElseThrow().getId();

        mockMvc.perform(get("/api/notifications").with(loginAs(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.unreadCount").value(2));

        mockMvc.perform(post("/api/notifications/{id}/read", othersId).with(loginAs(me)).with(csrf()))
                .andExpect(status().isNotFound());

        Long mine = notificationRepository.findAll().stream()
                .filter(n -> n.getUserId().equals(me.getId())).findFirst().orElseThrow().getId();
        mockMvc.perform(post("/api/notifications/{id}/read", mine).with(loginAs(me)).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications/unread-count").with(loginAs(me)))
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(post("/api/notifications/read-all").with(loginAs(me)).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications/unread-count").with(loginAs(me)))
                .andExpect(jsonPath("$.unreadCount").value(0));
        mockMvc.perform(get("/api/notifications/unread-count").with(loginAs(other)))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("로그인하지 않으면 알림을 볼 수 없다")
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }
}
