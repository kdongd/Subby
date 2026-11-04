package com.subby.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.subby.push.PushSender.SendResult;
import com.subby.support.IntegrationTestSupport;
import com.subby.user.User;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class PushIntegrationTest extends IntegrationTestSupport {

    private static final String ENDPOINT = "https://fcm.googleapis.com/fcm/send/device-1";
    private static final String P256DH = "BAvDis_3Rfel4GDVEBAvizXtKa12by0qtGT3UCYttSjmC79pkoyduVOlF8tDOR2W8N6Jl-kkggRsUQacIparIes";
    private static final String AUTH = "OSzrT-PR-1311pngWy9vzA";

    @Autowired
    private PushSubscriptionRepository repository;

    @BeforeEach
    void enablePush() {
        when(pushSender.isEnabled()).thenReturn(true);
        when(pushSender.publicKey()).thenReturn("test-public-key");
        when(pushSender.send(any(), any())).thenReturn(SendResult.SENT);
    }

    private String subscribeBody(String endpoint, String p256dh) throws Exception {
        return json(Map.of("endpoint", endpoint, "keys", Map.of("p256dh", p256dh, "auth", AUTH)));
    }

    private void subscribe(User user, String endpoint) throws Exception {
        mockMvc.perform(post("/api/push/subscriptions").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscribeBody(endpoint, P256DH)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("공개키를 내려준다")
    void publicKey() throws Exception {
        User user = createUser("key@example.com");
        mockMvc.perform(get("/api/push/public-key").with(loginAs(user)))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.publicKey").value("test-public-key"));
    }

    @Test
    @DisplayName("기기를 등록하고, 같은 기기를 다시 등록하면 하나로 유지한다")
    void subscribeIdempotent() throws Exception {
        User user = createUser("sub@example.com");

        subscribe(user, ENDPOINT);
        subscribe(user, ENDPOINT);

        assertThat(repository.count()).isEqualTo(1);
        mockMvc.perform(post("/api/push/status").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("endpoint", ENDPOINT))))
                .andExpect(jsonPath("$.subscribed").value(true))
                .andExpect(jsonPath("$.deviceCount").value(1));
    }

    @Test
    @DisplayName("같은 기기에서 다른 계정으로 등록하면 그 기기는 새 계정으로 옮겨진다")
    void reassignDevice() throws Exception {
        User first = createUser("first@example.com");
        User second = createUser("second@example.com");

        subscribe(first, ENDPOINT);
        subscribe(second, ENDPOINT);

        assertThat(repository.findByEndpoint(ENDPOINT).orElseThrow().getUserId()).isEqualTo(second.getId());
    }

    @Test
    @DisplayName("허용하지 않은 푸시 주소나 잘못된 키는 거부한다")
    void rejectsInvalid() throws Exception {
        User user = createUser("bad@example.com");

        mockMvc.perform(post("/api/push/subscriptions").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscribeBody("https://169.254.169.254/latest", P256DH)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mockMvc.perform(post("/api/push/subscriptions").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscribeBody(ENDPOINT, "not-a-key")))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("기기는 사용자당 10대까지만 남기고 오래된 것부터 지운다")
    void deviceLimit() throws Exception {
        User user = createUser("many@example.com");
        for (int i = 0; i < PushService.MAX_DEVICES_PER_USER + 2; i++) {
            subscribe(user, ENDPOINT + "-" + i);
        }

        assertThat(repository.countByUserId(user.getId())).isEqualTo(PushService.MAX_DEVICES_PER_USER);
        assertThat(repository.findByEndpoint(ENDPOINT + "-0")).isEmpty();
    }

    @Test
    @DisplayName("다른 사람의 기기는 해제할 수 없다")
    void unsubscribeOnlyMine() throws Exception {
        User owner = createUser("owner@example.com");
        User attacker = createUser("attacker@example.com");
        subscribe(owner, ENDPOINT);

        mockMvc.perform(post("/api/push/unsubscribe").with(loginAs(attacker)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("endpoint", ENDPOINT))))
                .andExpect(status().isNoContent());
        assertThat(repository.count()).isEqualTo(1);

        mockMvc.perform(post("/api/push/unsubscribe").with(loginAs(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("endpoint", ENDPOINT))))
                .andExpect(status().isNoContent());
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("테스트 푸시: 기기가 없으면 409, 있으면 보낸다")
    void testPush() throws Exception {
        User user = createUser("test@example.com");

        mockMvc.perform(post("/api/push/test").with(loginAs(user)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PUSH_NOT_SUBSCRIBED"));

        subscribe(user, ENDPOINT);
        mockMvc.perform(post("/api/push/test").with(loginAs(user)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices").value(1))
                .andExpect(jsonPath("$.sent").value(1));
    }
}
