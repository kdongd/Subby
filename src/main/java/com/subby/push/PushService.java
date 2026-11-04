package com.subby.push;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.push.PushDtos.Keys;
import com.subby.push.PushSender.SendResult;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    /** 한 사용자당 기기 수 상한. 넘으면 가장 오래된 기기부터 지운다. */
    static final int MAX_DEVICES_PER_USER = 10;

    private final PushSubscriptionRepository repository;
    private final PushSender pushSender;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public PushDtos.PublicKeyResponse publicKey() {
        return new PushDtos.PublicKeyResponse(pushSender.isEnabled(), pushSender.publicKey());
    }

    @Transactional
    public void subscribe(Long userId, String endpoint, Keys keys, String userAgent) {
        if (!PushEndpointPolicy.isAllowed(endpoint)) {
            throw ApiException.invalidField("endpoint", "지원하지 않는 브라우저의 푸시 주소입니다.");
        }
        validateKeys(keys);
        String agent = userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 300));

        repository.findByEndpoint(endpoint).ifPresentOrElse(
                existing -> existing.refresh(userId, keys.p256dh(), keys.auth(), agent),
                () -> repository.save(new PushSubscription(userId, endpoint, keys.p256dh(), keys.auth(), agent,
                        clock.instant())));
        repository.flush();

        List<PushSubscription> devices = repository.findAllByUserIdOrderByCreatedAtAscIdAsc(userId);
        int overflow = devices.size() - MAX_DEVICES_PER_USER;
        for (int i = 0; i < overflow; i++) {
            repository.delete(devices.get(i));
        }
    }

    /** 내 계정에 연결된 구독만 지운다. 다른 사람의 endpoint를 알아도 지울 수 없다. */
    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        repository.deleteByUserIdAndEndpoint(userId, endpoint);
    }

    @Transactional(readOnly = true)
    public PushDtos.StatusResponse status(Long userId, String endpoint) {
        boolean subscribed = endpoint != null && repository.existsByUserIdAndEndpoint(userId, endpoint);
        return new PushDtos.StatusResponse(subscribed, repository.countByUserId(userId));
    }

    public PushDtos.TestResponse sendTest(Long userId) {
        if (!pushSender.isEnabled()) {
            throw new ApiException(ErrorCode.PUSH_NOT_CONFIGURED);
        }
        List<PushSubscription> devices = repository.findAllByUserIdOrderByCreatedAtAscIdAsc(userId);
        if (devices.isEmpty()) {
            throw new ApiException(ErrorCode.PUSH_NOT_SUBSCRIBED);
        }
        int sent = sendToUser(userId,
                new PushMessage("서비 테스트 알림", "이 기기에서 결제 알림을 받을 수 있어요.", "/settings", "subby-test"));
        return new PushDtos.TestResponse(devices.size(), sent);
    }

    /**
     * 사용자의 모든 기기로 보낸다. 실패해도 예외를 던지지 않는다(사이트 안 알림은 이미 저장돼 있다).
     *
     * @return 성공한 기기 수
     */
    public int sendToUser(Long userId, PushMessage message) {
        if (!pushSender.isEnabled()) {
            return 0;
        }
        byte[] payload = toJson(message);
        int sent = 0;
        for (PushSubscription device : repository.findAllByUserIdOrderByCreatedAtAscIdAsc(userId)) {
            SendResult result = pushSender.send(device, payload);
            switch (result) {
                case SENT -> {
                    sent++;
                    transactionTemplate.executeWithoutResult(status -> repository.findById(device.getId())
                            .ifPresent(saved -> saved.markSuccess(clock.instant())));
                }
                case GONE -> {
                    log.info("만료된 푸시 구독 삭제 subscriptionId={}", device.getId());
                    transactionTemplate.executeWithoutResult(status -> repository.deleteById(device.getId()));
                }
                default -> {
                    // FAILED: 일시적 오류일 수 있으니 구독은 남겨 둔다.
                }
            }
        }
        return sent;
    }

    private byte[] toJson(PushMessage message) {
        try {
            return objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void validateKeys(Keys keys) {
        try {
            byte[] p256dh = WebPushCrypto.b64Decode(keys.p256dh());
            WebPushCrypto.decodePublicKey(p256dh);
            if (WebPushCrypto.b64Decode(keys.auth()).length != 16) {
                throw ApiException.invalidField("keys.auth", "푸시 구독 정보가 올바르지 않습니다.");
            }
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw ApiException.invalidField("keys", "푸시 구독 정보가 올바르지 않습니다.");
        }
    }
}
