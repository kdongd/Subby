package com.subby.push;

/** VAPID 키가 없을 때. 사이트 안 알림(🔔)은 그대로 동작하고 기기 푸시만 꺼진다. */
public class DisabledPushSender implements PushSender {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String publicKey() {
        return null;
    }

    @Override
    public SendResult send(PushSubscription subscription, byte[] payload) {
        return SendResult.DISABLED;
    }
}
