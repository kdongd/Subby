package com.subby.push;

import com.subby.global.config.AppProperties;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class PushConfig {

    @Bean
    public PushSender pushSender(AppProperties properties, Clock clock) {
        AppProperties.Push push = properties.push();
        if (push == null || !StringUtils.hasText(push.publicKey()) || !StringUtils.hasText(push.privateKey())) {
            log.warn("VAPID_PUBLIC_KEY 또는 VAPID_PRIVATE_KEY가 없어 기기 푸시를 보내지 않습니다. 사이트 안 알림은 동작합니다.");
            return new DisabledPushSender();
        }
        String publicKey = push.publicKey().trim();
        String privateKey = push.privateKey().trim();
        if (!WebPushCrypto.isKeyPair(publicKey, privateKey)) {
            // 서버를 멈추지는 않는다. 키 설정 실수로 사이트 전체가 안 뜨는 것보다 푸시만 꺼지는 편이 낫다.
            log.error("VAPID 공개키와 개인키가 한 쌍이 아니거나 형식이 틀립니다. 기기 푸시를 끕니다.");
            return new DisabledPushSender();
        }
        String subject = StringUtils.hasText(push.subject()) ? push.subject().trim() : properties.baseUrl();
        return new VapidPushSender(publicKey, privateKey, subject, clock);
    }
}
