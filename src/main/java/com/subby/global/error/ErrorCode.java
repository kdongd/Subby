package com.subby.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "요청을 처리할 권한이 없습니다. 새로고침 후 다시 시도해 주세요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    LOGIN_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다. 기존 방식으로 로그인한 뒤 설정에서 연결하세요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    IDENTITY_IN_USE(HttpStatus.CONFLICT, "이미 다른 계정에 연결된 소셜 계정입니다."),
    PROVIDER_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 같은 종류의 소셜 계정이 연결되어 있습니다."),
    IDENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "연결되지 않은 로그인 수단입니다."),
    LAST_LOGIN_METHOD(HttpStatus.CONFLICT, "마지막 로그인 수단은 해제할 수 없습니다."),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "링크가 만료되었거나 올바르지 않습니다. 비밀번호 찾기를 다시 진행해 주세요."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    PUSH_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "서버에 푸시 알림이 설정되지 않았습니다."),
    PUSH_NOT_SUBSCRIBED(HttpStatus.CONFLICT, "푸시를 받을 기기가 없습니다. 먼저 이 기기에서 푸시 받기를 켜 주세요."),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독을 찾을 수 없습니다."),
    ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 해지된 구독입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
