-- 회원
CREATE TABLE users (
    id                 BIGSERIAL PRIMARY KEY,
    email              VARCHAR(255) UNIQUE,
    password_hash      VARCHAR(100),
    nickname           VARCHAR(30)  NOT NULL,
    notify_enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    notify_days_before INTEGER      NOT NULL DEFAULT 1 CHECK (notify_days_before IN (1, 3, 7)),
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL
);

-- 소셜 로그인 연결
CREATE TABLE user_identities (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         VARCHAR(10)  NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_identity_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT uk_identity_user_provider UNIQUE (user_id, provider)
);

-- 구독
CREATE TABLE subscriptions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name          VARCHAR(50)   NOT NULL,
    category      VARCHAR(30)   NOT NULL,
    currency      VARCHAR(3)    NOT NULL,
    amount        NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    billing_cycle VARCHAR(10)   NOT NULL,
    start_date    DATE          NOT NULL,
    status        VARCHAR(10)   NOT NULL,
    canceled_at   DATE,
    memo          VARCHAR(200),
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_subscription_canceled
        CHECK ((status = 'CANCELED' AND canceled_at IS NOT NULL AND canceled_at >= start_date)
            OR (status = 'ACTIVE' AND canceled_at IS NULL))
);

CREATE INDEX idx_subscriptions_user_status ON subscriptions (user_id, status);

-- 비밀번호 재설정 토큰 (원문 대신 SHA-256 해시만 저장)
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash CHAR(64)    NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ
);

-- 로그인 실패 기록 (가입 여부와 무관하게 이메일 기준)
CREATE TABLE login_attempts (
    email        VARCHAR(255) PRIMARY KEY,
    failed_count INTEGER      NOT NULL,
    locked_until TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ  NOT NULL
);

-- 환율
CREATE TABLE exchange_rates (
    base      VARCHAR(3)     NOT NULL,
    quote     VARCHAR(3)     NOT NULL,
    rate_date DATE           NOT NULL,
    rate      NUMERIC(12, 4) NOT NULL,
    PRIMARY KEY (base, quote, rate_date)
);

-- 결제 알림 발송 기록 (같은 결제 건에 두 번 보내지 않도록)
CREATE TABLE notification_logs (
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT      NOT NULL REFERENCES subscriptions (id) ON DELETE CASCADE,
    payment_date    DATE        NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_notification_payment UNIQUE (subscription_id, payment_date)
);

-- Spring Session JDBC (로그인 세션 저장)
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36)     NOT NULL,
    SESSION_ID            CHAR(36)     NOT NULL,
    CREATION_TIME         BIGINT       NOT NULL,
    LAST_ACCESS_TIME      BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL INT          NOT NULL,
    EXPIRY_TIME           BIGINT       NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BYTEA        NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);
