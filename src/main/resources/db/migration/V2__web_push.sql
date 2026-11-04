-- 결제 알림을 메일에서 웹 푸시 + 사이트 안 알림으로 바꾼다.

-- 기기(브라우저)별 푸시 구독 정보. endpoint는 브라우저 회사(Google, Apple, Mozilla 등)의 푸시 서버 주소다.
CREATE TABLE push_subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint        VARCHAR(2048) NOT NULL UNIQUE,
    p256dh          VARCHAR(100)  NOT NULL,
    auth            VARCHAR(50)   NOT NULL,
    user_agent      VARCHAR(300),
    created_at      TIMESTAMPTZ   NOT NULL,
    last_success_at TIMESTAMPTZ
);

CREATE INDEX idx_push_subscription_user ON push_subscriptions (user_id);

-- 사이트 안 알림(🔔). 결제 1건(구독 + 결제일)당 하나만 만든다. 이 유니크 제약이 중복 알림을 막는다.
CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    subscription_id BIGINT       NOT NULL REFERENCES subscriptions (id) ON DELETE CASCADE,
    payment_date    DATE         NOT NULL,
    title           VARCHAR(100) NOT NULL,
    body            VARCHAR(300) NOT NULL,
    link            VARCHAR(200) NOT NULL,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_notification_subscription_payment UNIQUE (subscription_id, payment_date)
);

CREATE INDEX idx_notification_user_created ON notifications (user_id, created_at DESC);

-- 지금까지 메일로 보낸 결제 건은 사이트 알림을 다시 만들지 않도록 옮겨 둔다(이미 읽은 것으로 처리).
INSERT INTO notifications (user_id, subscription_id, payment_date, title, body, link, read_at, created_at)
SELECT s.user_id, l.subscription_id, l.payment_date,
       LEFT(s.name, 80) || ' 결제 예정', '메일로 안내한 결제입니다.',
       '/subscriptions/' || s.id, l.sent_at, l.sent_at
FROM notification_logs l
JOIN subscriptions s ON s.id = l.subscription_id;

DROP TABLE notification_logs;
