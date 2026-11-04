package com.subby.auth;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    /** 대소문자만 다른 이메일을 같은 계정으로 보기 위해 앞뒤 공백을 지우고 소문자로 바꾼다. */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
