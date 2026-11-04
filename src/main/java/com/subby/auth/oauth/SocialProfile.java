package com.subby.auth.oauth;

import com.subby.auth.EmailNormalizer;
import com.subby.user.AuthProvider;
import java.util.Map;

/**
 * 제공자마다 다른 사용자 정보 응답을 같은 형태로 바꾼 것.
 *
 * @param email 제공자가 인증을 마친 이메일만 담는다. 인증되지 않았거나 동의를 거부했으면 null.
 */
public record SocialProfile(AuthProvider provider, String providerUserId, String email, String nickname) {

    static final String DEFAULT_NICKNAME = "구독자";

    public static SocialProfile of(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> google(attributes);
            case KAKAO -> kakao(attributes);
        };
    }

    /** Google userinfo 응답: sub, name, email, email_verified */
    private static SocialProfile google(Map<String, Object> attributes) {
        String id = String.valueOf(attributes.get("sub"));
        String email = Boolean.TRUE.equals(attributes.get("email_verified"))
                ? EmailNormalizer.normalize((String) attributes.get("email"))
                : null;
        return new SocialProfile(AuthProvider.GOOGLE, id, email, nickname((String) attributes.get("name")));
    }

    /** 카카오 사용자 정보 응답: id, kakao_account.email, kakao_account.is_email_verified, kakao_account.profile.nickname */
    @SuppressWarnings("unchecked")
    private static SocialProfile kakao(Map<String, Object> attributes) {
        String id = String.valueOf(attributes.get("id"));
        Map<String, Object> account = attributes.get("kakao_account") instanceof Map<?, ?> accountMap
                ? (Map<String, Object>) accountMap
                : Map.of();
        Map<String, Object> profile = account.get("profile") instanceof Map<?, ?> profileMap
                ? (Map<String, Object>) profileMap
                : Map.of();
        boolean verified = Boolean.TRUE.equals(account.get("is_email_valid"))
                && Boolean.TRUE.equals(account.get("is_email_verified"));
        String email = verified ? EmailNormalizer.normalize((String) account.get("email")) : null;
        return new SocialProfile(AuthProvider.KAKAO, id, email, nickname((String) profile.get("nickname")));
    }

    private static String nickname(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_NICKNAME;
        }
        String trimmed = raw.trim();
        return trimmed.length() > 30 ? trimmed.substring(0, 30) : trimmed;
    }
}
