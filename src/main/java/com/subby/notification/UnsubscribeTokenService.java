package com.subby.notification;

import com.subby.global.config.AppProperties;
import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.global.security.SecureTokens;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * 메일 하단 "알림 끄기" 링크용 토큰. 로그인하지 않아도 동작해야 하므로 사용자 ID에 서버 비밀값으로 서명한다.
 * 서명이 맞지 않으면 다른 사람의 알림을 끌 수 없다. 형식: {userId}.{HMAC-SHA256 서명}
 */
@Service
public class UnsubscribeTokenService {

    private static final String PURPOSE = "unsubscribe:";

    private final byte[] secret;

    public UnsubscribeTokenService(AppProperties properties) {
        this.secret = properties.unsubscribeSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String create(Long userId) {
        return userId + "." + sign(userId);
    }

    public Long verify(String token) {
        if (token == null) {
            throw new ApiException(ErrorCode.INVALID_UNSUBSCRIBE_TOKEN);
        }
        int dot = token.indexOf('.');
        if (dot <= 0) {
            throw new ApiException(ErrorCode.INVALID_UNSUBSCRIBE_TOKEN);
        }
        long userId;
        try {
            userId = Long.parseLong(token.substring(0, dot));
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.INVALID_UNSUBSCRIBE_TOKEN);
        }
        if (!SecureTokens.constantTimeEquals(sign(userId), token.substring(dot + 1))) {
            throw new ApiException(ErrorCode.INVALID_UNSUBSCRIBE_TOKEN);
        }
        return userId;
    }

    private String sign(long userId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] signature = mac.doFinal((PURPOSE + userId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
